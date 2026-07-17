"use strict";

/*
 * =============================================================================
 *  PAGINA DI LOGIN  (index.html)
 * =============================================================================
 *
 *  Riscritta come "classe di oggetti": il comportamento della pagina e'
 *  incapsulato nel costruttore LoginController. L'oggetto e' esso stesso il
 *  listener degli eventi (metodo handleEvent), cosi' "this" e' sempre il
 *  controller anche dentro i gestori. Le chiamate al server passano per
 *  TIW.Http (vedi utils.js).
 * =============================================================================
 */

(function () {

    /* ---------------------------------------------------------------------- *
     *  LoginController - stato e comportamento della pagina di login          *
     * ---------------------------------------------------------------------- */
    function LoginController() {
        this.http = new TIW.Http();

        this.loginForm = document.getElementById("loginForm");
        this.roleChoice = document.getElementById("roleChoice");
        this.errorMessage = document.getElementById("errormessage");
        this.loginButton = document.getElementById("loginbutton");

        /* bottoni della scelta ruolo: creati dinamicamente, riferimenti a null. */
        this.goManagerBtn = null;
        this.goCollaboratorBtn = null;

        /* L'oggetto stesso e' il listener (handleEvent). */
        this.loginButton.addEventListener("click", this);
    }

    /* Dispatcher unico: instrada in base all'elemento che ha generato l'evento. */
    LoginController.prototype.handleEvent = function (event) {
        switch (event.currentTarget) {
            case this.loginButton:        return this.onLoginClick(event);
            case this.goManagerBtn:       return this.goTo("homeManager");
            case this.goCollaboratorBtn:  return this.goTo("homeCollaboratorJS");
        }
    };

    /* Invio delle credenziali. */
    LoginController.prototype.onLoginClick = function (event) {
        var form = event.currentTarget.closest("form");
        if (!form.checkValidity()) {
            form.reportValidity();
            return;
        }

        var self = this;
        this.http.request({
            method: "POST",
            url: "CheckLogin",
            body: new FormData(form),
            onSuccess: function (data) {
                sessionStorage.setItem("username", data.username);
                self.route(data.position);
            },
            onError: function (message) {
                self.errorMessage.textContent = message;
            }
        });
        form.reset();
    };

    /* Instrada in base al ruolo restituito dal server. */
    LoginController.prototype.route = function (position) {
        switch (position) {
            case "ADMIN":        this.goTo("homeAdmin");           break; // nuovo DOM
            case "MANAGER":      this.goTo("homeManager");         break; // nuovo DOM
            case "COLLABORATOR": this.goTo("homeCollaboratorJS");  break; // nuovo DOM
            case "TECHNICIAN":   this.showRoleChoice();            break; // resta, muta il DOM
            default:
                this.errorMessage.textContent = "Ruolo non riconosciuto";
        }
    };

    LoginController.prototype.goTo = function (page) {
        window.location.href = page;
    };

    /*
     * Unico caso senza redirect: l'utente e' sia responsabile sia collaboratore
     * e sceglie con quale home entrare. Il DOM viene mutato senza ricaricare.
     */
    LoginController.prototype.showRoleChoice = function () {
        this.loginForm.style.display = "none";

        TIW.dom.clear(this.roleChoice);
        this.roleChoice.appendChild(TIW.dom.el("h2", { text: "Come vuoi accedere?" }));
        this.goManagerBtn = TIW.dom.el("button", { id: "goManager", text: "Home Responsabile" });
        this.goCollaboratorBtn = TIW.dom.el("button", { id: "goCollaborator", text: "Home Collaboratore" });
        this.roleChoice.appendChild(this.goManagerBtn);
        this.roleChoice.appendChild(document.createTextNode(" "));
        this.roleChoice.appendChild(this.goCollaboratorBtn);
        this.roleChoice.style.display = "block";

        /* Anche qui i listener sono l'oggetto controller (handleEvent). */
        this.goManagerBtn.addEventListener("click", this);
        this.goCollaboratorBtn.addEventListener("click", this);
    };

    /* Avvio della pagina. */
    new LoginController();

})();