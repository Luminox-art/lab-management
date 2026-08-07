(function () {
    "use strict";

    function validationMessage(control) {
        if (control.validity.valueMissing) {
            return "Vui lòng nhập thông tin bắt buộc này.";
        }
        if (control.validity.typeMismatch) {
            return "Giá trị chưa đúng định dạng yêu cầu.";
        }
        if (control.validity.tooShort) {
            return "Nội dung cần ít nhất " + control.minLength + " ký tự.";
        }
        if (control.validity.rangeUnderflow) {
            return "Giá trị phải từ " + control.min + " trở lên.";
        }
        if (control.validity.rangeOverflow) {
            return "Giá trị không được vượt quá " + control.max + ".";
        }
        return control.validationMessage || "Giá trị chưa hợp lệ.";
    }

    function validateControl(control) {
        if (!control.willValidate) {
            return;
        }
        var field = control.closest(".field");
        if (!field) {
            return;
        }
        var error = field.querySelector(".field-error");
        if (!control.validity.valid) {
            if (!error) {
                error = document.createElement("p");
                error.className = "field-error";
                error.setAttribute("role", "alert");
                field.appendChild(error);
            }
            if (!error.id) {
                error.id = control.id + "-client-error";
            }
            error.textContent = validationMessage(control);
            control.setAttribute("aria-invalid", "true");
            control.setAttribute("aria-describedby", error.id);
            return;
        }
        control.removeAttribute("aria-invalid");
        if (error && error.id.endsWith("-client-error")) {
            error.remove();
        } else if (error) {
            error.textContent = "";
        }
    }

    function setLoading(form, submitter) {
        if (!submitter || submitter.disabled) {
            return;
        }
        submitter.disabled = true;
        submitter.setAttribute("aria-busy", "true");
        submitter.classList.add("button-loading");
        submitter.dataset.originalLabel = submitter.textContent;
        submitter.textContent = "Đang xử lý…";
        var status = document.createElement("span");
        status.className = "sr-only";
        status.setAttribute("role", "status");
        status.textContent = "Yêu cầu đang được xử lý.";
        form.appendChild(status);
    }

    function createConfirmationDialog() {
        var dialog = document.createElement("dialog");
        dialog.className = "confirm-dialog";
        dialog.innerHTML = '<div class="dialog-content" role="document">'
            + '<h2 id="confirm-dialog-heading">Xác nhận thao tác</h2>'
            + '<p id="confirm-dialog-message"></p>'
            + '<div class="dialog-actions">'
            + '<button class="button button-secondary" type="button" data-dialog-cancel>Quay lại</button>'
            + '<button class="button button-danger" type="button" data-dialog-confirm>Xác nhận</button>'
            + '</div></div>';
        dialog.setAttribute("aria-labelledby", "confirm-dialog-heading");
        dialog.setAttribute("aria-describedby", "confirm-dialog-message");
        document.body.appendChild(dialog);
        return dialog;
    }

    function initializeSidebar() {
        var sidebar = document.querySelector("[data-sidebar]");
        var toggle = document.querySelector("[data-sidebar-toggle]");
        var dismiss = document.querySelector("[data-sidebar-dismiss]");
        if (!sidebar || !toggle || !dismiss) {
            return;
        }

        function setOpen(open, restoreFocus) {
            document.body.classList.toggle("nav-open", open);
            toggle.setAttribute("aria-expanded", String(open));
            toggle.setAttribute("aria-label", open ? "Đóng menu điều hướng" : "Mở menu điều hướng");
            if (!open && restoreFocus) {
                toggle.focus();
            }
        }

        toggle.addEventListener("click", function () {
            setOpen(!document.body.classList.contains("nav-open"), false);
        });
        dismiss.addEventListener("click", function () {
            setOpen(false, true);
        });
        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape" && document.body.classList.contains("nav-open")) {
                setOpen(false, true);
            }
        });

        var desktop = window.matchMedia("(min-width: 64.001rem)");
        desktop.addEventListener("change", function (event) {
            if (event.matches) {
                setOpen(false, false);
            }
        });
    }

    document.addEventListener("DOMContentLoaded", function () {
        initializeSidebar();

        var firstInvalid = document.querySelector('[aria-invalid="true"]');
        if (firstInvalid) {
            firstInvalid.focus();
        }

        document.querySelectorAll("[data-password-toggle]").forEach(function (button) {
            button.addEventListener("click", function () {
                var input = document.getElementById(button.getAttribute("aria-controls"));
                var visible = input.type === "text";
                input.type = visible ? "password" : "text";
                button.textContent = visible ? "Hiện" : "Ẩn";
                button.setAttribute("aria-pressed", String(!visible));
                input.focus();
            });
        });

        document.querySelectorAll("input, select, textarea").forEach(function (control) {
            control.addEventListener("blur", function () { validateControl(control); });
            control.addEventListener("change", function () { validateControl(control); });
        });

        var dialog = createConfirmationDialog();
        var pendingForm = null;
        var pendingSubmitter = null;
        var cancelButton = dialog.querySelector("[data-dialog-cancel]");
        var confirmButton = dialog.querySelector("[data-dialog-confirm]");

        cancelButton.addEventListener("click", function () {
            dialog.close();
            pendingForm = null;
            pendingSubmitter = null;
        });
        dialog.addEventListener("cancel", function () {
            pendingForm = null;
            pendingSubmitter = null;
        });
        confirmButton.addEventListener("click", function () {
            dialog.close();
            var form = pendingForm;
            var submitter = pendingSubmitter;
            pendingForm = null;
            pendingSubmitter = null;
            form.dataset.confirmed = "true";
            if (submitter) {
                form.requestSubmit(submitter);
            } else {
                form.requestSubmit();
            }
        });

        document.addEventListener("submit", function (event) {
            if (event.defaultPrevented) {
                return;
            }
            var form = event.target;
            if (form.dataset.confirm && form.dataset.confirmed !== "true") {
                event.preventDefault();
                pendingForm = form;
                pendingSubmitter = event.submitter;
                dialog.querySelector("#confirm-dialog-message").textContent = form.dataset.confirm;
                dialog.showModal();
                cancelButton.focus();
                return;
            }
            delete form.dataset.confirmed;
            setLoading(form, event.submitter);
        });
    });
}());
