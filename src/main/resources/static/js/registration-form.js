(function () {
    "use strict";

    const form = document.querySelector("[data-registration-form]");
    if (!form) {
        return;
    }

    const scheduleList = form.querySelector("#schedule-list");
    const scheduleTemplate = form.querySelector("#schedule-row-template");
    const addScheduleButton = form.querySelector("[data-add-schedule]");
    const scheduleAnnouncement = form.querySelector("#schedule-announcement");

    function scheduleRows() {
        return Array.from(scheduleList.querySelectorAll("[data-schedule-row]"));
    }

    function connectError(field, index, kind) {
        const error = field.closest(".field").querySelector(".field-error");
        if (!error) {
            field.removeAttribute("aria-describedby");
            field.removeAttribute("aria-invalid");
            return;
        }
        error.id = `schedule-${kind}-error-${index}`;
        field.setAttribute("aria-describedby", error.id);
    }

    function reindexSchedules() {
        const rows = scheduleRows();
        rows.forEach(function (row, index) {
            const number = index + 1;
            const day = row.querySelector("[data-schedule-day]");
            const period = row.querySelector("[data-schedule-period]");
            const dayLabel = row.querySelector("[data-day-label]");
            const periodLabel = row.querySelector("[data-period-label]");
            const legend = row.querySelector("[data-schedule-legend]");
            const removeButton = row.querySelector("[data-remove-schedule]");

            day.name = `schedules[${index}].dayOfWeek`;
            day.id = `schedule-day-${index}`;
            dayLabel.htmlFor = day.id;
            connectError(day, index, "day");

            period.name = `schedules[${index}].periodId`;
            period.id = `schedule-period-${index}`;
            periodLabel.htmlFor = period.id;
            connectError(period, index, "period");

            legend.textContent = `Lịch ${number}`;
            removeButton.hidden = rows.length === 1;
            removeButton.setAttribute("aria-label", `Xóa lịch ${number}`);
        });
    }

    function announce(message) {
        if (scheduleAnnouncement) {
            scheduleAnnouncement.textContent = message;
        }
    }

    if (scheduleList && scheduleTemplate && addScheduleButton) {
        addScheduleButton.hidden = false;
        addScheduleButton.addEventListener("click", function () {
            const row = scheduleTemplate.content.firstElementChild.cloneNode(true);
            scheduleList.appendChild(row);
            reindexSchedules();
            row.querySelector("[data-schedule-day]").focus();
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
            const remainingRows = scheduleRows();
            const focusRow = remainingRows[Math.min(removedIndex, remainingRows.length - 1)];
            focusRow.querySelector("[data-schedule-day]").focus();
            announce("Đã xóa một dòng lịch.");
        });

        form.addEventListener("submit", reindexSchedules);
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

    const supervisorSection = form.querySelector("[data-supervisor-section]");
    const supervisorInput = form.querySelector("[data-supervisor-input]");
    const supervisorDevices = form.querySelectorAll("[data-instructor-required='true']");

    function updateSupervisorSection() {
        if (!supervisorSection || !supervisorInput) {
            return;
        }
        const required = Array.from(supervisorDevices).some(function (device) {
            return device.checked;
        });
        supervisorSection.hidden = !required;
        supervisorInput.required = required;
    }

    supervisorDevices.forEach(function (device) {
        device.addEventListener("change", updateSupervisorSection);
    });
    updateSupervisorSection();
}());
