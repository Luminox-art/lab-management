(function () {
    "use strict";

    const MAX_SCHEDULES = 128;
    const form = document.querySelector("[data-registration-form]");
    if (!form) {
        return;
    }

    const scheduleList = form.querySelector("#schedule-list");
    const scheduleTemplate = form.querySelector("#schedule-row-template");
    const addScheduleButton = form.querySelector("[data-add-schedule]");
    const scheduleAnnouncement = form.querySelector("#schedule-announcement");
    const availabilityStatus = form.querySelector("[data-availability-status]");
    const startDateInput = form.querySelector("#startDate");
    const endDateInput = form.querySelector("#endDate");
    const roomSelect = form.querySelector("#roomId");
    const roomCalendarLink = form.querySelector("[data-room-calendar-link]");
    let availabilityChecking = false;
    let roomCalendar = null;
    let roomCalendarKey = "";
    let roomCalendarState = "idle";
    let roomCalendarRequest = 0;
    let roomCalendarRefreshTimer = null;

    function scheduleRows() {
        return scheduleList ? Array.from(scheduleList.querySelectorAll("[data-schedule-row]")) : [];
    }

    function connectError(field, index, kind) {
        const error = field.closest(".field").querySelector(".field-error");
        if (!error) {
            field.removeAttribute("aria-describedby");
            return;
        }
        error.id = `schedule-${kind}-error-${index}`;
        field.setAttribute("aria-describedby", error.id);
    }

    function rememberServerValidation(field) {
        if (!Object.prototype.hasOwnProperty.call(field.dataset, "serverInvalid")) {
            field.dataset.serverInvalid = field.getAttribute("aria-invalid") === "true" ? "true" : "false";
        }
    }

    function reindexSchedules() {
        const rows = scheduleRows();
        rows.forEach(function (row, index) {
            const number = index + 1;
            const day = row.querySelector("[data-schedule-day]");
            const periodStart = row.querySelector("[data-period-start]");
            const periodEnd = row.querySelector("[data-period-end]");
            const dayLabel = row.querySelector("[data-day-label]");
            const startPeriodLabel = row.querySelector("[data-start-period-label]");
            const endPeriodLabel = row.querySelector("[data-end-period-label]");
            const legend = row.querySelector("[data-schedule-legend]");
            const removeButton = row.querySelector("[data-remove-schedule]");

            rememberServerValidation(day);
            rememberServerValidation(periodStart);
            rememberServerValidation(periodEnd);
            day.name = `schedules[${index}].dayOfWeek`;
            day.id = `schedule-day-${index}`;
            dayLabel.htmlFor = day.id;
            connectError(day, index, "day");

            periodStart.name = `schedules[${index}].startPeriodId`;
            periodStart.id = `schedule-period-start-${index}`;
            startPeriodLabel.htmlFor = periodStart.id;
            connectError(periodStart, index, "period-start");

            periodEnd.name = `schedules[${index}].endPeriodId`;
            periodEnd.id = `schedule-period-end-${index}`;
            endPeriodLabel.htmlFor = periodEnd.id;
            connectError(periodEnd, index, "period-end");

            legend.textContent = `Lịch ${number}`;
            removeButton.hidden = rows.length === 1;
            removeButton.setAttribute("aria-label", `Xóa lịch ${number}`);
            rememberPeriodOptions(row);
            applyRoomCalendarToRow(row);
            updateRangeSummary(row);
        });
        if (addScheduleButton) {
            addScheduleButton.disabled = rows.length >= MAX_SCHEDULES;
        }
    }

    function announce(message) {
        if (scheduleAnnouncement) {
            scheduleAnnouncement.textContent = message;
        }
    }

    function clearClientInvalid(field) {
        if (field.dataset.clientInvalid !== "true") {
            return;
        }
        delete field.dataset.clientInvalid;
        if (field.dataset.serverInvalid !== "true") {
            field.removeAttribute("aria-invalid");
        }
    }

    function markClientInvalid(field) {
        field.dataset.clientInvalid = "true";
        field.setAttribute("aria-invalid", "true");
    }

    function periodOptions(row) {
        return Array.from(row.querySelector("[data-period-start]").options).filter(function (option) {
            return option.value;
        });
    }

    function rememberPeriodOptions(row) {
        row.querySelectorAll("[data-period-start] option[value], [data-period-end] option[value]").forEach(function (option) {
            if (option.value && !option.dataset.baseLabel) {
                option.dataset.baseLabel = option.textContent;
            }
        });
    }

    function optionIsRoomUnavailable(option) {
        return option && option.dataset.roomUnavailable === "true";
    }

    function periodIndex(row, value) {
        return periodOptions(row).findIndex(function (option) {
            return option.value === value;
        });
    }

    function selectedRange(row) {
        const start = row.querySelector("[data-period-start]");
        const end = row.querySelector("[data-period-end]");
        return {
            start: start,
            end: end,
            startIndex: periodIndex(row, start.value),
            endIndex: periodIndex(row, end.value)
        };
    }

    function updateRangeOptions(row, clearInvalidEnd) {
        const range = selectedRange(row);
        const options = periodOptions(row);
        Array.from(range.start.options).forEach(function (option) {
            option.disabled = Boolean(option.value) && optionIsRoomUnavailable(option);
        });
        Array.from(range.end.options).forEach(function (option) {
            if (!option.value) {
                option.disabled = false;
                return;
            }
            const optionIndex = periodIndex(row, option.value);
            const crossesUnavailable = range.startIndex >= 0 && optionIndex >= range.startIndex
                && options.slice(range.startIndex, optionIndex + 1).some(optionIsRoomUnavailable);
            option.disabled = optionIsRoomUnavailable(option)
                || (range.startIndex >= 0 && optionIndex < range.startIndex)
                || crossesUnavailable;
        });
        const selectedEnd = range.end.selectedOptions.length ? range.end.selectedOptions[0] : null;
        if (clearInvalidEnd && selectedEnd && selectedEnd.disabled) {
            range.end.value = "";
        }
    }

    function updateRangeSummary(row) {
        const summary = row.querySelector("[data-schedule-range-summary]");
        const range = selectedRange(row);
        if (!summary || range.startIndex < 0 || range.endIndex < range.startIndex) {
            if (summary) {
                summary.textContent = "";
            }
            return;
        }
        const startOption = range.start.selectedOptions[0];
        const endOption = range.end.selectedOptions[0];
        const periodCount = range.endIndex - range.startIndex + 1;
        const periodLabel = periodCount === 1
            ? startOption.dataset.periodName
            : `${startOption.dataset.periodName}–${endOption.dataset.periodName}`;
        summary.textContent = `${periodLabel} (${startOption.dataset.startTime}–${endOption.dataset.endTime}) · ${periodCount} tiết`;
    }

    function calendarSelectionKey() {
        if (!roomSelect || !roomSelect.value || !startDateInput.value || !endDateInput.value
                || startDateInput.value > endDateInput.value) {
            return "";
        }
        return `${roomSelect.value}|${startDateInput.value}|${endDateInput.value}`;
    }

    function formatCalendarDate(value) {
        const parts = String(value || "").split("-");
        return parts.length === 3 ? `${parts[2]}/${parts[1]}/${parts[0]}` : String(value || "");
    }

    function describeBusyEvents(events) {
        const byDate = new Map();
        events.forEach(function (event) {
            const titles = byDate.get(event.date) || new Set();
            titles.add(event.title || "Không khả dụng");
            byDate.set(event.date, titles);
        });
        return Array.from(byDate.entries()).sort(function (left, right) {
            return left[0].localeCompare(right[0]);
        }).map(function (entry) {
            return `${formatCalendarDate(entry[0])} (${Array.from(entry[1]).join(", ")})`;
        }).join(", ");
    }

    function roomEventsFor(row, periodId, includeAllDay) {
        if (!roomCalendar) {
            return [];
        }
        const day = row.querySelector("[data-schedule-day]").value;
        return (roomCalendar.events || []).filter(function (event) {
            return String(event.dayOfWeek) === day
                && ((includeAllDay && event.allDay) || String(event.periodId) === String(periodId));
        });
    }

    function setRoomAvailabilityStatus(row, message, state) {
        const status = row.querySelector("[data-schedule-room-availability]");
        if (!status) {
            return;
        }
        status.textContent = message;
        if (state) {
            status.dataset.state = state;
        } else {
            delete status.dataset.state;
        }
    }

    function resetRoomPeriodOptions(row) {
        row.querySelectorAll("[data-period-start] option[value], [data-period-end] option[value]").forEach(function (option) {
            if (!option.value) {
                return;
            }
            option.textContent = option.dataset.baseLabel || option.textContent;
            option.removeAttribute("title");
            delete option.dataset.roomUnavailable;
        });
    }

    function markPeriodUnavailable(row, periodId, events) {
        const dates = Array.from(new Set(events.map(function (event) {
            return event.date;
        }))).sort();
        const shortLabel = dates.length === 1 ? formatCalendarDate(dates[0]) : `${dates.length} ngày`;
        ["[data-period-start]", "[data-period-end]"].forEach(function (selector) {
            const option = Array.from(row.querySelector(selector).options).find(function (candidate) {
                return candidate.value === String(periodId);
            });
            if (!option) {
                return;
            }
            option.dataset.roomUnavailable = "true";
            option.textContent = `${option.dataset.baseLabel || option.textContent} — Bận ${shortLabel}`;
            option.title = `Không khả dụng: ${describeBusyEvents(events)}`;
        });
    }

    function roomConflictSummary(row) {
        const day = row.querySelector("[data-schedule-day]").value;
        if (!roomCalendar || !day) {
            return "";
        }
        const parts = [];
        const allDayEvents = (roomCalendar.events || []).filter(function (event) {
            return String(event.dayOfWeek) === day && event.allDay;
        });
        if (allDayEvents.length) {
            parts.push(`Cả ngày: ${describeBusyEvents(allDayEvents)}`);
        }
        periodOptions(row).forEach(function (option) {
            const events = roomEventsFor(row, option.value, false);
            if (events.length) {
                parts.push(`${option.dataset.periodName}: ${describeBusyEvents(events)}`);
            }
        });
        return parts.join("; ");
    }

    function rangeContainsUnavailablePeriod(row) {
        const range = selectedRange(row);
        return range.startIndex >= 0 && range.endIndex >= range.startIndex
            && periodOptions(row).slice(range.startIndex, range.endIndex + 1).some(optionIsRoomUnavailable);
    }

    function applyRoomCalendarToRow(row) {
        rememberPeriodOptions(row);
        resetRoomPeriodOptions(row);
        const key = calendarSelectionKey();
        const day = row.querySelector("[data-schedule-day]").value;
        if (!key) {
            setRoomAvailabilityStatus(row, "Chọn phòng và khoảng ngày để xem các tiết đã bận.", "");
            updateRangeOptions(row, false);
            return;
        }
        if (!day) {
            setRoomAvailabilityStatus(row, "Chọn thứ để xem các tiết đã bận của phòng.", "");
            updateRangeOptions(row, false);
            return;
        }
        if (roomCalendarState === "loading" || roomCalendarKey !== key) {
            setRoomAvailabilityStatus(row, "Đang tải lịch bận của phòng…", "loading");
            updateRangeOptions(row, false);
            return;
        }
        if (roomCalendarState === "error" || !roomCalendar) {
            setRoomAvailabilityStatus(row, "Không thể tải lịch bận. Hệ thống vẫn kiểm tra lại khi gửi phiếu.", "error");
            updateRangeOptions(row, false);
            return;
        }

        periodOptions(row).forEach(function (option) {
            const events = roomEventsFor(row, option.value, true);
            if (events.length) {
                markPeriodUnavailable(row, option.value, events);
            }
        });

        const range = selectedRange(row);
        if (rangeContainsUnavailablePeriod(row)) {
            const startOption = range.start.selectedOptions.length ? range.start.selectedOptions[0] : null;
            if (optionIsRoomUnavailable(startOption)) {
                range.start.value = "";
            }
            range.end.value = "";
            announce("Đã bỏ chọn khoảng tiết vì lịch phòng vừa cập nhật có xung đột.");
        }
        updateRangeOptions(row, false);
        updateRangeSummary(row);
        const conflict = roomConflictSummary(row);
        setRoomAvailabilityStatus(row, conflict ? `Không khả dụng — ${conflict}.` : "Tất cả tiết của thứ này đang khả dụng.",
            conflict ? "conflict" : "available");
    }

    function rangeContainsSystemDay(from, to, systemDay) {
        if (!from || !to || from > to || !systemDay) {
            return true;
        }
        const start = new Date(`${from}T00:00:00Z`);
        const end = new Date(`${to}T00:00:00Z`);
        const javaDay = start.getUTCDay() === 0 ? 7 : start.getUTCDay();
        const targetJavaDay = Number(systemDay) === 8 ? 7 : Number(systemDay) - 1;
        const daysUntilTarget = (targetJavaDay - javaDay + 7) % 7;
        start.setUTCDate(start.getUTCDate() + daysUntilTarget);
        return start <= end;
    }

    function validateSchedules(focusFirst) {
        const rows = scheduleRows();
        let firstInvalid = null;
        let expandedScheduleCount = 0;

        rows.forEach(function (row) {
            const day = row.querySelector("[data-schedule-day]");
            const periodStart = row.querySelector("[data-period-start]");
            const periodEnd = row.querySelector("[data-period-end]");
            const error = row.querySelector("[data-schedule-client-error]");
            clearClientInvalid(day);
            clearClientInvalid(periodStart);
            clearClientInvalid(periodEnd);
            error.textContent = "";
            error.hidden = true;
            const range = selectedRange(row);
            if (range.startIndex >= 0 && range.endIndex >= 0) {
                if (range.endIndex < range.startIndex) {
                    markClientInvalid(periodEnd);
                    error.textContent = "Tiết kết thúc phải bằng hoặc sau tiết bắt đầu.";
                    error.hidden = false;
                    firstInvalid = firstInvalid || periodEnd;
                } else if (rangeContainsUnavailablePeriod(row)) {
                    markClientInvalid(periodEnd);
                    error.textContent = "Khoảng tiết chứa tiết không khả dụng của phòng.";
                    error.hidden = false;
                    firstInvalid = firstInvalid || periodEnd;
                } else {
                    expandedScheduleCount += range.endIndex - range.startIndex + 1;
                }
            }
        });

        rows.forEach(function (row, index) {
            const day = row.querySelector("[data-schedule-day]");
            const range = selectedRange(row);
            if (!day.value || range.startIndex < 0 || range.endIndex < range.startIndex) {
                return;
            }
            rows.slice(0, index).forEach(function (candidate) {
                const candidateDay = candidate.querySelector("[data-schedule-day]");
                const candidateRange = selectedRange(candidate);
                if (candidateDay.value !== day.value || candidateRange.startIndex < 0
                        || candidateRange.endIndex < candidateRange.startIndex
                        || range.startIndex > candidateRange.endIndex
                        || candidateRange.startIndex > range.endIndex) {
                    return;
                }
                [candidate, row].forEach(function (overlappingRow) {
                    const end = overlappingRow.querySelector("[data-period-end]");
                    const error = overlappingRow.querySelector("[data-schedule-client-error]");
                    markClientInvalid(end);
                    error.textContent = "Khoảng tiết này giao với một lịch khác.";
                    error.hidden = false;
                    firstInvalid = firstInvalid || end;
                });
            });
        });

        rows.forEach(function (row) {
            const day = row.querySelector("[data-schedule-day]");
            const error = row.querySelector("[data-schedule-client-error]");
            if (error.hidden && !rangeContainsSystemDay(startDateInput.value, endDateInput.value, day.value)) {
                markClientInvalid(day);
                error.textContent = "Khoảng ngày không chứa thứ đã chọn.";
                error.hidden = false;
                firstInvalid = firstInvalid || day;
            }
        });

        if (expandedScheduleCount > MAX_SCHEDULES) {
            const lastRow = rows[rows.length - 1];
            const end = lastRow.querySelector("[data-period-end]");
            const error = lastRow.querySelector("[data-schedule-client-error]");
            markClientInvalid(end);
            error.textContent = "Một phiếu không được có quá 128 tiết sử dụng.";
            error.hidden = false;
            firstInvalid = firstInvalid || end;
        }

        if (focusFirst && firstInvalid) {
            firstInvalid.focus();
        }
        return !firstInvalid;
    }

    function clearAvailability() {
        scheduleRows().forEach(function (row) {
            const error = row.querySelector("[data-schedule-availability-error]");
            error.textContent = "";
            error.hidden = true;
        });
        if (availabilityStatus) {
            availabilityStatus.hidden = true;
            availabilityStatus.textContent = "";
            availabilityStatus.classList.remove("message-error", "message-success");
        }
    }

    function setAvailabilityStatus(message, kind) {
        if (!availabilityStatus) {
            return;
        }
        availabilityStatus.textContent = message;
        availabilityStatus.hidden = false;
        availabilityStatus.classList.toggle("message-error", kind === "error");
        availabilityStatus.classList.toggle("message-success", kind === "success");
    }

    if (scheduleList && scheduleTemplate && addScheduleButton) {
        addScheduleButton.hidden = false;
        addScheduleButton.addEventListener("click", function () {
            const rows = scheduleRows();
            if (rows.length >= MAX_SCHEDULES) {
                announce("Một phiếu chỉ được có tối đa 128 lịch sử dụng.");
                return;
            }
            const previousDay = rows.length ? rows[rows.length - 1].querySelector("[data-schedule-day]").value : "";
            const row = scheduleTemplate.content.firstElementChild.cloneNode(true);
            row.querySelector("[data-schedule-day]").value = previousDay;
            row.querySelector("[data-period-start]").value = "";
            row.querySelector("[data-period-end]").value = "";
            scheduleList.appendChild(row);
            reindexSchedules();
            clearAvailability();
            row.querySelector(previousDay ? "[data-period-start]" : "[data-schedule-day]").focus();
            announce(`Đã thêm lịch ${scheduleRows().length}.`);
        });

        scheduleList.addEventListener("click", function (event) {
            const removeButton = event.target.closest("[data-remove-schedule]");
            if (!removeButton || scheduleRows().length === 1) {
                return;
            }
            const row = removeButton.closest("[data-schedule-row]");
            const removedIndex = scheduleRows().indexOf(row);
            row.remove();
            reindexSchedules();
            validateSchedules(false);
            clearAvailability();
            const remainingRows = scheduleRows();
            const focusRow = remainingRows[Math.min(removedIndex, remainingRows.length - 1)];
            focusRow.querySelector("[data-schedule-day]").focus();
            announce("Đã xóa một dòng lịch.");
        });

        scheduleList.addEventListener("change", function (event) {
            if (event.target.matches("[data-schedule-day], [data-period-start], [data-period-end]")) {
                const row = event.target.closest("[data-schedule-row]");
                if (event.target.matches("[data-schedule-day]")) {
                    applyRoomCalendarToRow(row);
                } else {
                    updateRangeOptions(row, event.target.matches("[data-period-start]"));
                }
                updateRangeSummary(row);
                validateSchedules(false);
                clearAvailability();
            }
        });
        [startDateInput, endDateInput].forEach(function (input) {
            input.addEventListener("change", function () {
                validateSchedules(false);
                clearAvailability();
                updateRoomCalendarLink();
                scheduleRoomCalendarRefresh();
            });
        });
        reindexSchedules();
    }

    const typeSelect = form.querySelector("[data-registration-type]");
    const teachingSection = form.querySelector("[data-teaching-section]");
    const teachingInputs = form.querySelectorAll("[data-teaching-required]");

    function updateTeachingSection() {
        if (!typeSelect || !teachingSection) {
            return;
        }
        const teaching = typeSelect.value === "GIANG_DAY";
        teachingSection.hidden = !teaching;
        teachingInputs.forEach(function (input) {
            input.required = teaching;
        });
    }

    if (typeSelect) {
        typeSelect.addEventListener("change", updateTeachingSection);
        updateTeachingSection();
    }

    const participantInput = form.querySelector("#participantCount");
    const participantHelp = form.querySelector("#participant-help");
    const deviceFilterStatus = form.querySelector("[data-device-filter-status]");
    const deviceInputs = Array.from(form.querySelectorAll(".device-option input[type='checkbox']"));
    const supervisorSection = form.querySelector("[data-supervisor-section]");
    const supervisorInput = form.querySelector("[data-supervisor-input]");

    function updateSupervisorSection() {
        if (!supervisorSection || !supervisorInput) {
            return;
        }
        const required = deviceInputs.some(function (device) {
            return !device.disabled && device.dataset.instructorRequired === "true" && device.checked;
        });
        supervisorSection.hidden = !required;
        supervisorInput.required = required;
    }

    function updateParticipantLimit() {
        const option = roomSelect && roomSelect.selectedOptions.length ? roomSelect.selectedOptions[0] : null;
        const capacity = option && option.dataset.capacity ? Number(option.dataset.capacity) : null;
        if (!participantInput) {
            return;
        }
        participantInput.setCustomValidity("");
        participantInput.removeAttribute("max");
        if (!capacity) {
            participantHelp.textContent = "Chọn phòng để áp dụng giới hạn sức chứa.";
            return;
        }
        participantInput.max = String(capacity);
        participantHelp.textContent = `Phòng đã chọn có sức chứa tối đa ${capacity} người.`;
        if (participantInput.value && Number(participantInput.value) > capacity) {
            participantInput.setCustomValidity(`Số người không được vượt quá sức chứa ${capacity} của phòng.`);
        }
    }

    function updateDevicesForRoom() {
        const selectedRoom = roomSelect ? roomSelect.value : "";
        let removedCount = 0;
        deviceInputs.forEach(function (device) {
            const label = device.closest(".device-option");
            const compatible = !selectedRoom || device.dataset.mobile === "true"
                || device.dataset.roomId === selectedRoom;
            if (!compatible && device.checked) {
                device.checked = false;
                removedCount += 1;
            }
            device.disabled = !compatible;
            label.hidden = !compatible;
        });
        if (deviceFilterStatus) {
            deviceFilterStatus.textContent = removedCount
                ? `Đã bỏ chọn ${removedCount} thiết bị cố định không thuộc phòng mới.`
                : (selectedRoom ? "Chỉ hiển thị thiết bị cố định thuộc phòng đã chọn và thiết bị di động." : "");
        }
        updateSupervisorSection();
    }

    function updateRoomCalendarLink() {
        if (!roomCalendarLink || !roomSelect || !roomSelect.value) {
            if (roomCalendarLink) {
                roomCalendarLink.hidden = true;
            }
            return;
        }
        const params = new URLSearchParams({roomId: roomSelect.value});
        if (startDateInput.value) {
            params.set("from", startDateInput.value);
        }
        if (endDateInput.value) {
            params.set("to", endDateInput.value);
        }
        roomCalendarLink.href = `/schedule/calendar?${params.toString()}`;
        roomCalendarLink.hidden = false;
    }

    function roomCalendarUrl() {
        const params = new URLSearchParams({from: startDateInput.value, to: endDateInput.value});
        return `/api/v1/rooms/${encodeURIComponent(roomSelect.value)}/calendar?${params.toString()}`;
    }

    function refreshRoomCalendar() {
        const key = calendarSelectionKey();
        const request = ++roomCalendarRequest;
        if (!key) {
            roomCalendar = null;
            roomCalendarKey = "";
            roomCalendarState = "idle";
            if (scheduleList) {
                scheduleList.removeAttribute("aria-busy");
            }
            scheduleRows().forEach(applyRoomCalendarToRow);
            return;
        }
        roomCalendar = null;
        roomCalendarKey = key;
        roomCalendarState = "loading";
        if (scheduleList) {
            scheduleList.setAttribute("aria-busy", "true");
        }
        scheduleRows().forEach(applyRoomCalendarToRow);
        fetch(roomCalendarUrl(), {headers: {Accept: "application/json"}}).then(function (response) {
            if (!response.ok) {
                throw new Error("room-calendar-request-failed");
            }
            return response.json();
        }).then(function (payload) {
            if (request !== roomCalendarRequest) {
                return;
            }
            roomCalendar = payload.data;
            roomCalendarState = "loaded";
            if (scheduleList) {
                scheduleList.removeAttribute("aria-busy");
            }
            scheduleRows().forEach(applyRoomCalendarToRow);
            validateSchedules(false);
        }).catch(function () {
            if (request !== roomCalendarRequest) {
                return;
            }
            roomCalendar = null;
            roomCalendarState = "error";
            if (scheduleList) {
                scheduleList.removeAttribute("aria-busy");
            }
            scheduleRows().forEach(applyRoomCalendarToRow);
        });
    }

    function scheduleRoomCalendarRefresh() {
        if (roomCalendarRefreshTimer) {
            window.clearTimeout(roomCalendarRefreshTimer);
        }
        roomCalendarRefreshTimer = window.setTimeout(refreshRoomCalendar, 150);
    }

    if (roomSelect) {
        roomSelect.addEventListener("change", function () {
            updateParticipantLimit();
            updateDevicesForRoom();
            clearAvailability();
            updateRoomCalendarLink();
            scheduleRoomCalendarRefresh();
        });
    }
    if (participantInput) {
        participantInput.addEventListener("input", updateParticipantLimit);
    }
    deviceInputs.forEach(function (device) {
        device.addEventListener("change", function () {
            updateSupervisorSection();
            clearAvailability();
        });
    });
    updateParticipantLimit();
    updateDevicesForRoom();
    updateRoomCalendarLink();
    scheduleRoomCalendarRefresh();

    function availabilityUrl(row, periodId) {
        const params = new URLSearchParams();
        params.set("roomId", roomSelect.value);
        params.set("from", startDateInput.value);
        params.set("to", endDateInput.value);
        params.set("dayOfWeek", row.querySelector("[data-schedule-day]").value);
        params.set("periodId", periodId);
        deviceInputs.filter(function (device) {
            return !device.disabled && device.checked;
        }).forEach(function (device) {
            params.append("deviceIds", device.value);
        });
        return `/api/v1/availability?${params.toString()}`;
    }

    function availabilityTargets() {
        return scheduleRows().flatMap(function (row) {
            const range = selectedRange(row);
            if (range.startIndex < 0 || range.endIndex < range.startIndex) {
                return [];
            }
            return periodOptions(row).slice(range.startIndex, range.endIndex + 1).map(function (option) {
                return {row: row, periodId: option.value, periodName: option.dataset.periodName};
            });
        });
    }

    async function checkAvailability() {
        const results = await Promise.all(availabilityTargets().map(async function (target) {
            const row = target.row;
            const periodId = target.periodId;
            const response = await fetch(availabilityUrl(row, periodId), {headers: {Accept: "application/json"}});
            if (!response.ok) {
                throw new Error("availability-request-failed");
            }
            return {row: row, periodName: target.periodName, availability: (await response.json()).data};
        }));
        let conflictCount = 0;
        const conflictsByRow = new Map();
        results.forEach(function (result) {
            if (result.availability.available) {
                return;
            }
            conflictCount += 1;
            const conflicts = result.availability.conflicts || [];
            const message = conflicts.length ? conflicts[0].message : "Lịch đã chọn không khả dụng.";
            const rowConflicts = conflictsByRow.get(result.row) || [];
            rowConflicts.push(`${result.periodName}: ${message}`);
            conflictsByRow.set(result.row, rowConflicts);
        });
        conflictsByRow.forEach(function (conflicts, row) {
            const error = row.querySelector("[data-schedule-availability-error]");
            const suffix = conflicts.length > 1 ? ` (và ${conflicts.length - 1} tiết xung đột khác)` : "";
            error.textContent = conflicts[0] + suffix;
            error.hidden = false;
        });
        return conflictCount;
    }

    form.addEventListener("submit", function (event) {
        reindexSchedules();
        if (form.dataset.availabilityConfirmed === "true") {
            delete form.dataset.availabilityConfirmed;
            return;
        }
        if (!validateSchedules(true)) {
            event.preventDefault();
            announce("Vui lòng sửa các dòng lịch chưa hợp lệ.");
            return;
        }
        if (availabilityChecking) {
            event.preventDefault();
            return;
        }

        event.preventDefault();
        clearAvailability();
        availabilityChecking = true;
        form.setAttribute("aria-busy", "true");
        setAvailabilityStatus("Đang kiểm tra khả dụng của phòng và thiết bị…", "");
        const submitter = event.submitter;
        checkAvailability().then(function (conflictCount) {
            if (conflictCount) {
                setAvailabilityStatus(`Có ${conflictCount} tiết đang xung đột. Vui lòng chọn khoảng tiết khác.`, "error");
                const firstError = scheduleRows().find(function (row) {
                    return !row.querySelector("[data-schedule-availability-error]").hidden;
                });
                firstError.querySelector("[data-period-end]").focus();
                return;
            }
            setAvailabilityStatus("Phòng và thiết bị đang khả dụng. Đang gửi phiếu…", "success");
            form.dataset.availabilityConfirmed = "true";
            if (submitter) {
                form.requestSubmit(submitter);
            } else {
                form.requestSubmit();
            }
        }).catch(function () {
            setAvailabilityStatus("Không thể kiểm tra khả dụng lúc này. Vui lòng thử lại.", "error");
        }).finally(function () {
            availabilityChecking = false;
            form.removeAttribute("aria-busy");
        });
    });
}());
