"use strict";

/*
 * =============================================================================
 *  HOME RESPONSABILE  (homeManager.html) - interfaccia single page
 * =============================================================================
 *
 *  Riscrittura in stile "classi di oggetti":
 *
 *    - ManagerApi ............ "classe" che incapsula il context path e tutte le
 *                              chiamate asincrone (lettura GET e azioni POST).
 *    - HomeView .............. vista 1: assegnazione collaboratori e ore previste.
 *    - ProjectMonitorView .... vista 2: monitoraggio progetti + conclusione.
 *    - CollaboratorMonitorView vista 3: monitoraggio collaboratori.
 *    - ManagerApp ............ orchestratore: naviga tra le viste e le istanzia.
 *
 *  Ogni vista e' un oggetto con stato (this.*) e usa se stessa come listener
 *  (handleEvent) per i propri controlli. Le utilita' comuni sono in TIW.dom e
 *  TIW.Http (utils.js).
 *
 *    - letture: GET  <context>/GetManagerData?resource=...
 *    - azioni:  POST <context>/ManagerActionJS  (action=save|assign|conclude)
 * =============================================================================
 */

(function () {

    var dom = TIW.dom;

    /* Trasforma i progetti in opzioni {value, label} per le <select>. */
    function optionsFromProjects(projects) {
        return projects.map(function (p) {
            return { value: p.titolo, label: p.titolo + " (stato: " + p.stato + ")" };
        });
    }

    /* ====================================================================== *
     *  ManagerApi - incapsula context path e chiamate al server               *
     * ====================================================================== */
    function ManagerApi(ctx) {
        this.ctx = ctx || "/";
        this.loginUrl = this.ctx + "login";
        this.http = new TIW.Http();
    }

    ManagerApi.prototype.urlFor = function (resource, params) {
        var url = this.ctx + "GetManagerData?resource=" + encodeURIComponent(resource);
        if (params) {
            var keys = Object.keys(params);
            for (var i = 0; i < keys.length; i++) {
                url += "&" + encodeURIComponent(keys[i]) + "=" + encodeURIComponent(params[keys[i]]);
            }
        }
        return url;
    };

    /* GET verso GetManagerData. */
    ManagerApi.prototype.get = function (resource, params, onOk, onErr) {
        var self = this;
        this.http.request({
            method: "GET",
            url: this.urlFor(resource, params),
            headers: { "Accept": "application/json" },
            onUnauthorized: function () { window.location.href = self.loginUrl; },
            onSuccess: function (data) { onOk(data); },
            onError: function (message) { if (onErr) { onErr(message); } }
        });
    };

    /* POST verso ManagerActionJS; pairs = array di [chiave, valore]. */
    ManagerApi.prototype.action = function (pairs, onOk, onErr) {
        var self = this;
        this.http.request({
            method: "POST",
            url: this.ctx + "ManagerActionJS",
            headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
            body: dom.encodeParams(pairs),
            onUnauthorized: function () { window.location.href = self.loginUrl; },
            onSuccess: function (data) { onOk(data); },
            onError: function (message) { if (onErr) { onErr(message); } }
        });
    };

    /* ====================================================================== *
     *  VISTA 1 - HomeView: assegnazione collaboratori e ore previste           *
     * ====================================================================== */
    function HomeView(api) {
        this.api = api;
        this.loaded = false;
        this.state = { progetto: null, wp: null, task: null, months: [], editable: false };

        this.projectSel = document.getElementById("home-project");
        this.wpSel = document.getElementById("home-wp");
        this.taskSel = document.getElementById("home-task");
        this.statusEl = document.getElementById("home-status");
        this.detailEl = document.getElementById("home-detail");
        this.actionsEl = document.getElementById("home-actions");
        this.msgEl = document.getElementById("home-msg");
        this.saveBtn = document.getElementById("btn-save");
        this.assignBtn = document.getElementById("btn-assign");

        this.projectSel.addEventListener("change", this);
        this.wpSel.addEventListener("change", this);
        this.taskSel.addEventListener("change", this);
        this.saveBtn.addEventListener("click", this);
        this.assignBtn.addEventListener("click", this);
    }

    HomeView.prototype.handleEvent = function (event) {
        switch (event.currentTarget) {
            case this.projectSel: return this.onProjectChange();
            case this.wpSel:      return this.onWpChange();
            case this.taskSel:    return this.onTaskChange();
            case this.saveBtn:    return this.onSave();
            case this.assignBtn:  return this.onAssign();
        }
    };

    /* Caricamento pigro dell'elenco progetti (una sola volta). */
    HomeView.prototype.ensureLoaded = function () {
        if (this.loaded) {
            return;
        }
        this.loaded = true;
        var self = this;
        this.api.get("projects", null, function (projects) {
            dom.fillSelect(self.projectSel, optionsFromProjects(projects));
        }, function (message) {
            self.loaded = false;
            dom.showMsg(self.msgEl, message, true);
        });
    };

    HomeView.prototype.onProjectChange = function () {
        dom.resetSelect(this.wpSel, true);
        dom.resetSelect(this.taskSel, true);
        dom.clear(this.detailEl);
        this.actionsEl.hidden = true;
        this.statusEl.textContent = "";
        dom.showMsg(this.msgEl, "");

        var progetto = this.projectSel.value;
        this.state = { progetto: progetto || null, wp: null, task: null, months: [], editable: false };
        if (!progetto) {
            return;
        }

        var self = this;
        this.api.get("wps", { progetto: progetto }, function (wps) {
            dom.fillSelect(self.wpSel, wps.map(function (w) { return { value: w.id, label: w.label }; }));
            self.wpSel.disabled = false;
        }, function (message) {
            dom.showMsg(self.msgEl, message, true);
        });
    };

    HomeView.prototype.onWpChange = function () {
        dom.resetSelect(this.taskSel, true);
        dom.clear(this.detailEl);
        this.actionsEl.hidden = true;
        dom.showMsg(this.msgEl, "");

        this.state.wp = this.wpSel.value ? parseInt(this.wpSel.value, 10) : null;
        this.state.task = null;
        if (!this.state.wp) {
            return;
        }

        var self = this;
        this.api.get("tasks", { progetto: this.state.progetto, wp: this.state.wp }, function (tasks) {
            dom.fillSelect(self.taskSel, tasks.map(function (t) { return { value: t.id, label: t.label }; }));
            self.taskSel.disabled = false;
        }, function (message) {
            dom.showMsg(self.msgEl, message, true);
        });
    };

    HomeView.prototype.onTaskChange = function () {
        dom.clear(this.detailEl);
        this.actionsEl.hidden = true;
        dom.showMsg(this.msgEl, "");

        this.state.task = this.taskSel.value ? parseInt(this.taskSel.value, 10) : null;
        if (!this.state.task) {
            return;
        }

        var self = this;
        this.api.get("taskDetail",
            { progetto: this.state.progetto, wp: this.state.wp, task: this.state.task },
            function (detail) { self.renderTaskDetail(detail); },
            function (message) { dom.showMsg(self.msgEl, message, true); });
    };

    HomeView.prototype.renderTaskDetail = function (detail) {
        this.state.months = detail.months || [];
        this.state.editable = !!detail.editable;
        this.statusEl.textContent = "Stato del progetto: " + detail.status +
            (detail.editable ? "" : " (sola lettura)");

        if (this.state.months.length === 0) {
            this.detailEl.appendChild(dom.el("p", {
                "class": "muted", text: "Il task non ha un intervallo di mesi valido."
            }));
            return;
        }

        this.detailEl.appendChild(dom.el("h3", { text: "Ore previste (mese per mese)" }));
        var table = dom.el("table", { "class": "grid" });
        var headRow = dom.el("tr");
        var valRow = dom.el("tr");
        dom.each(this.state.months, function (m) {
            headRow.appendChild(dom.el("th", { text: "M" + m }));
            var input = dom.el("input", { type: "number", min: "0", id: "ore_" + m });
            var planned = detail.plannedHours ? detail.plannedHours[m] : undefined;
            if (planned !== undefined && planned !== null) {
                input.value = planned;
            }
            if (!detail.editable) {
                input.disabled = true;
            }
            valRow.appendChild(dom.el("td", null, [input]));
        });
        table.appendChild(headRow);
        table.appendChild(valRow);
        this.detailEl.appendChild(table);

        this.detailEl.appendChild(dom.el("h3", { text: "Collaboratori incaricati" }));
        var assigned = detail.assignedIds || [];
        var detailEl = this.detailEl;
        if (!detail.collaborators || detail.collaborators.length === 0) {
            this.detailEl.appendChild(dom.el("p", {
                "class": "muted", text: "Nessun collaboratore disponibile."
            }));
        } else {
            dom.each(detail.collaborators, function (c) {
                var cb = dom.el("input", { type: "checkbox", value: String(c.id), "class": "collab-cb" });
                if (assigned.indexOf(c.id) !== -1) {
                    cb.checked = true;
                }
                if (!detail.editable) {
                    cb.disabled = true;
                }
                var label = dom.el("label", null, [cb, document.createTextNode(" " + c.nome + " " + c.cognome)]);
                detailEl.appendChild(label);
                detailEl.appendChild(dom.el("br"));
            });
        }

        this.actionsEl.hidden = !detail.editable;
    };

    HomeView.prototype.onSave = function () {
        if (!this.state.task) {
            return;
        }
        var pairs = [];
        dom.each(this.state.months, function (m) {
            var input = document.getElementById("ore_" + m);
            if (input && input.value !== "") {
                pairs.push(["ore_" + m, input.value]);
            }
        });
        dom.each(this.detailEl.querySelectorAll(".collab-cb"), function (cb) {
            if (cb.checked) {
                pairs.push(["collaboratore", cb.value]);
            }
        });
        pairs.push(["action", "save"]);
        pairs.push(["progetto", this.state.progetto]);
        pairs.push(["wp", this.state.wp]);
        pairs.push(["task", this.state.task]);

        var self = this;
        this.api.action(pairs, function (result) {
            if (result && result.success) {
                dom.showMsg(self.msgEl, "Dati salvati correttamente.", false);
            } else {
                dom.showMsg(self.msgEl, (result && result.error) || "Salvataggio non riuscito.", true);
            }
        }, function (message) {
            dom.showMsg(self.msgEl, message, true);
        });
    };

    HomeView.prototype.onAssign = function () {
        if (!this.state.progetto) {
            return;
        }
        var self = this;
        var pairs = [["action", "assign"], ["progetto", this.state.progetto]];
        this.api.action(pairs, function (result) {
            if (result && result.success) {
                dom.showMsg(self.msgEl, "Progetto assegnato con successo.", false);
                // Il progetto diventa 'assegnato' -> ricarico il dettaglio in sola
                // lettura. I dati non si perdono in caso di errore perche' restano
                // nel DOM finche' non si ricarica il dettaglio.
                if (self.state.task) {
                    self.onTaskChange();
                }
            } else if (result && result.problems && result.problems.length > 0) {
                dom.showMsg(self.msgEl, "Impossibile assegnare: " + result.problems.join(" | "), true);
            } else {
                dom.showMsg(self.msgEl, (result && result.error) || "Assegnamento non riuscito.", true);
            }
        }, function (message) {
            dom.showMsg(self.msgEl, message, true);
        });
    };

    /* ====================================================================== *
     *  VISTA 2 - ProjectMonitorView: monitoraggio progetti                     *
     * ====================================================================== */
    function ProjectMonitorView(api) {
        this.api = api;
        this.loaded = false;
        this.current = null;

        this.select = document.getElementById("proj-select");
        this.tableEl = document.getElementById("proj-table");
        this.actionsEl = document.getElementById("proj-actions");
        this.msgEl = document.getElementById("proj-msg");
        this.concludeBtn = document.getElementById("btn-conclude");

        this.select.addEventListener("change", this);
        this.concludeBtn.addEventListener("click", this);
    }

    ProjectMonitorView.prototype.handleEvent = function (event) {
        switch (event.currentTarget) {
            case this.select:      return this.onSelectChange();
            case this.concludeBtn: return this.onConclude();
        }
    };

    ProjectMonitorView.prototype.ensureLoaded = function () {
        if (this.loaded) {
            return;
        }
        this.loaded = true;
        var self = this;
        this.api.get("projects", null, function (projects) {
            dom.fillSelect(self.select, optionsFromProjects(projects));
        }, function (message) {
            self.loaded = false;
            dom.showMsg(self.msgEl, message, true);
        });
    };

    ProjectMonitorView.prototype.onSelectChange = function () {
        dom.clear(this.tableEl);
        this.actionsEl.hidden = true;
        dom.showMsg(this.msgEl, "");
        this.current = this.select.value || null;
        if (!this.current) {
            return;
        }
        this.load();
    };

    ProjectMonitorView.prototype.load = function () {
        var self = this;
        this.api.get("projectMonitoring", { progetto: this.current }, function (data) {
            self.render(data);
        }, function (message) {
            dom.showMsg(self.msgEl, message, true);
        });
    };

    ProjectMonitorView.prototype.render = function (data) {
        dom.clear(this.tableEl);

        if (!data.wpGroups || data.wpGroups.length === 0) {
            this.tableEl.appendChild(dom.el("p", {
                "class": "muted", text: "Il progetto non contiene WP o task."
            }));
            this.actionsEl.hidden = true;
            return;
        }

        var table = dom.el("table", { "class": "grid" });
        var head = dom.el("tr");
        head.appendChild(dom.el("th", { text: "WP / Task" }));
        dom.each(data.months, function (m) {
            head.appendChild(dom.el("th", { text: "M" + m + " prev." }));
            head.appendChild(dom.el("th", { text: "M" + m + " lav." }));
        });
        table.appendChild(head);

        dom.each(data.wpGroups, function (wp) {
            var wpRow = dom.el("tr", { "class": "wp-row" });
            var wpCell = dom.el("td", { text: wp.label });
            wpCell.setAttribute("colspan", String(data.months.length * 2 + 1));
            wpRow.appendChild(wpCell);
            table.appendChild(wpRow);

            dom.each(wp.tasks, function (task) {
                var tr = dom.el("tr");
                tr.appendChild(dom.el("td", { "class": "task-label", text: task.label }));
                dom.each(task.cells, function (cell) {
                    tr.appendChild(dom.el("td", { text: String(cell.previste) }));
                    tr.appendChild(dom.el("td", { text: String(cell.lavorate) }));
                });
                table.appendChild(tr);
            });
        });
        this.tableEl.appendChild(table);

        var canConclude = data.concludable && data.status && data.status.toLowerCase() === "assegnato";
        this.actionsEl.hidden = !canConclude;

        if (data.status && data.status.toLowerCase() === "concluso") {
            this.tableEl.appendChild(dom.el("p", { "class": "msg-ok", text: "Il progetto e' gia' concluso." }));
        }
    };

    ProjectMonitorView.prototype.onConclude = function () {
        if (!this.current) {
            return;
        }
        var self = this;
        var pairs = [["action", "conclude"], ["progetto", this.current]];
        this.api.action(pairs, function (result) {
            if (result && result.success) {
                dom.showMsg(self.msgEl, "Progetto concluso con successo.", false);
                self.load();
            } else {
                dom.showMsg(self.msgEl, (result && result.error) || "Conclusione non riuscita.", true);
            }
        }, function (message) {
            dom.showMsg(self.msgEl, message, true);
        });
    };

    /* ====================================================================== *
     *  VISTA 3 - CollaboratorMonitorView: monitoraggio collaboratori           *
     * ====================================================================== */
    function CollaboratorMonitorView(api) {
        this.api = api;
        this.loaded = false;

        this.select = document.getElementById("collab-select");
        this.tablesEl = document.getElementById("collab-tables");
        this.msgEl = document.getElementById("collab-msg");

        this.select.addEventListener("change", this);
    }

    CollaboratorMonitorView.prototype.handleEvent = function (event) {
        if (event.currentTarget === this.select) {
            this.onSelectChange();
        }
    };

    CollaboratorMonitorView.prototype.ensureLoaded = function () {
        if (this.loaded) {
            return;
        }
        this.loaded = true;
        var self = this;
        this.api.get("collaborators", null, function (people) {
            dom.fillSelect(self.select, people.map(function (c) {
                return { value: c.id, label: c.nome + " " + c.cognome };
            }));
            if (people.length === 0) {
                dom.showMsg(self.msgEl, "Nessun collaboratore lavora nei tuoi progetti.", false);
            }
        }, function (message) {
            self.loaded = false;
            dom.showMsg(self.msgEl, message, true);
        });
    };

    CollaboratorMonitorView.prototype.onSelectChange = function () {
        dom.clear(this.tablesEl);
        dom.showMsg(this.msgEl, "");
        var id = this.select.value;
        if (!id) {
            return;
        }
        var self = this;
        this.api.get("collaboratorMonitoring", { collaboratore: id }, function (data) {
            self.render(data);
        }, function (message) {
            dom.showMsg(self.msgEl, message, true);
        });
    };

    CollaboratorMonitorView.prototype.render = function (data) {
        dom.clear(this.tablesEl);
        this.tablesEl.appendChild(dom.el("h3", { text: "Collaboratore: " + data.collaboratorName }));

        if (!data.projects || data.projects.length === 0) {
            this.tablesEl.appendChild(dom.el("p", {
                "class": "muted",
                text: "Questo collaboratore non lavora in alcun tuo progetto."
            }));
            return;
        }

        var tablesEl = this.tablesEl;
        dom.each(data.projects, function (proj) {
            tablesEl.appendChild(dom.el("h4", { text: "Progetto: " + proj.title }));
            var table = dom.el("table", { "class": "grid" });

            var head = dom.el("tr");
            head.appendChild(dom.el("th", { text: "WP / Task" }));
            dom.each(proj.months, function (m) {
                head.appendChild(dom.el("th", { text: "M" + m }));
            });
            table.appendChild(head);

            dom.each(proj.wpGroups, function (wp) {
                var wpRow = dom.el("tr", { "class": "wp-row" });
                var wpCell = dom.el("td", { text: wp.label });
                wpCell.setAttribute("colspan", String(proj.months.length + 1));
                wpRow.appendChild(wpCell);
                table.appendChild(wpRow);

                dom.each(wp.tasks, function (task) {
                    var tr = dom.el("tr");
                    tr.appendChild(dom.el("td", { "class": "task-label", text: task.label }));
                    dom.each(task.worked, function (w) {
                        tr.appendChild(dom.el("td", { text: String(w) }));
                    });
                    table.appendChild(tr);
                });
            });
            tablesEl.appendChild(table);
        });
    };

    /* ====================================================================== *
     *  ManagerApp - orchestratore: navigazione tra le viste                    *
     * ====================================================================== */
    function ManagerApp(api) {
        this.api = api;
        this.navButtons = document.querySelectorAll("#mainNav .nav-btn");
        this.views = document.querySelectorAll(".view");
        this.greetingEl = document.getElementById("greeting");
        this.photoEl = document.getElementById("userPhoto");

        this.home = new HomeView(api);
        this.projects = new ProjectMonitorView(api);
        this.collaborators = new CollaboratorMonitorView(api);

        // Logout gestito dall'oggetto-listener condiviso (utils.js).
        this.logout = new TIW.LogoutController("logoutBtn");

        var self = this;
        dom.each(this.navButtons, function (btn) {
            btn.addEventListener("click", self);
        });

        this.loadProfile();
        this.showView("view-home");
    }

    /* Carica il profilo del responsabile (saluto personalizzato + foto). */
    ManagerApp.prototype.loadProfile = function () {
        var self = this;
        this.api.get("me", null, function (me) {
            if (me && (me.name || me.surname)) {
                self.greetingEl.textContent = "Home Responsabile - " +
                    ((me.name || "") + " " + (me.surname || "")).trim();
            }
            if (me && me.photoUrl) {
                self.showPhoto(me.photoUrl);
            }
        }, function () {
            /* silenzioso: senza profilo/foto resta il saluto statico */
        });
    };

    /*
     * Mostra la foto del tecnico solo se caricata con successo: l'<img> parte
     * nascosto, compare al "load" e resta nascosto in caso di "error".
     */
    ManagerApp.prototype.showPhoto = function (url) {
        var img = this.photoEl;
        if (!img || !url) { return; }
        img.addEventListener("load", function () { img.hidden = false; });
        img.addEventListener("error", function () { img.hidden = true; });
        img.src = url;
    };

    ManagerApp.prototype.handleEvent = function (event) {
        // I bottoni di navigazione hanno tutti come listener l'app.
        this.showView(event.currentTarget.getAttribute("data-view"));
    };

    ManagerApp.prototype.showView = function (id) {
        dom.each(this.views, function (v) {
            v.hidden = (v.id !== id);
        });
        dom.each(this.navButtons, function (b) {
            b.className = (b.getAttribute("data-view") === id) ? "nav-btn active" : "nav-btn";
        });

        if (id === "view-home") {
            this.home.ensureLoaded();
        } else if (id === "view-projects") {
            this.projects.ensureLoaded();
        } else if (id === "view-collaborators") {
            this.collaborators.ensureLoaded();
        }
    };

    /* ----------------------------- avvio ---------------------------------- */
    new ManagerApp(new ManagerApi(window.APP_CONTEXT || "/"));

})();