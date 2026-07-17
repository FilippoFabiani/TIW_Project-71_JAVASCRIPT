"use strict";

/*
 * =============================================================================
 *  HOME COLLABORATORE  (homeCollaboratorJS.html) - interfaccia single page
 * =============================================================================
 *
 *  Comportamento (da specifica, versione JavaScript):
 *
 *    - Tutti i dati (nome, progetti, WP, task, ore lavorate) sono caricati UNA
 *      SOLA VOLTA all'apertura della pagina, con una chiamata asincrona a
 *      GetCollaboratorData che restituisce un CollaboratorBoard in JSON.
 *    - La selezione di un progetto avviene interamente lato client (nessuna
 *      nuova richiesta): si mostra la relativa tabella "ore lavorate".
 *    - Una cella modificabile si edita cliccandola; il valore viene salvato
 *      automaticamente quando il puntatore esce dalla cella (evento blur),
 *      inviando la sola cella a UpdateWorkedHoursJS. In caso di errore di
 *      validazione (lato client o server) il valore precedente viene ripristinato.
 *
 *  Realizzazione in stile "classi di oggetti":
 *
 *    - CollaboratorApi ... incapsula context path e chiamate al server.
 *    - ProjectTable ...... "classe" che disegna e gestisce la tabella di UN
 *                          progetto, editing delle celle compreso (handleEvent).
 *    - CollaboratorApp ... orchestratore: carica i dati, costruisce l'elenco
 *                          progetti e mostra la tabella selezionata.
 *
 *  JSON di GetCollaboratorData:
 *    { collaboratorName,
 *      projects: [ { title, duration, months:[..],
 *                    wps: [ { label,
 *                             tasks: [ { wp, task, label,
 *                                        cells: [ { month, worked, editable } ] } ] } ] } ] }
 * =============================================================================
 */

(function () {

    var dom = TIW.dom;

    /* ====================================================================== *
     *  CollaboratorApi - chiamate al server                                    *
     * ====================================================================== */
    class CollaboratorApi {
        constructor(ctx) {
            this.ctx = ctx || "/";
            this.loginUrl = this.ctx + "login";
            this.http = new TIW.Http();
        }
        /* Carica in un'unica chiamata l'intero board del collaboratore. */
        loadBoard(onOk, onErr) {
            var self = this;
            this.http.request({
                method: "GET",
                url: this.ctx + "GetCollaboratorData",
                headers: { "Accept": "application/json" },
                onUnauthorized: function() { window.location.href = self.loginUrl; },
                onSuccess: function(data) { onOk(data); },
                onError: function(message) { if (onErr) { onErr(message); } }
            });
        }
        /* Salva le ore lavorate di una singola cella (task, mese). */
        saveHours(progetto, wp, task, mese, ore, onOk, onErr) {
            var self = this;
            var pairs = [
                ["progetto", progetto],
                ["wp", wp],
                ["task", task],
                ["mese", mese],
                ["ore", ore]
            ];
            this.http.request({
                method: "POST",
                url: this.ctx + "UpdateWorkedHoursJS",
                headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
                body: dom.encodeParams(pairs),
                onUnauthorized: function() { window.location.href = self.loginUrl; },
                onSuccess: function(data) { onOk(data); },
                onError: function(message) { if (onErr) { onErr(message); } }
            });
        }
    }



    /* ====================================================================== *
     *  ProjectTable - disegna e gestisce la tabella di un singolo progetto     *
     * ====================================================================== *
     *
     *  E' anche il listener (handleEvent) degli eventi della tabella:
     *    - click su una cella editabile  -> avvio editing
     *    - blur / Invio / Esc sull'input -> conferma o annulla
     */
    class ProjectTable {
        constructor(api, project, onError) {
            this.api = api;
            this.project = project;
            this.onError = onError; // callback per mostrare errori globali
            this.editing = null; // { td, prev, wp, task, month } quando si edita
            this.tableEl = this.build();
            this.tableEl.addEventListener("click", this);
        }
        /* Nodo radice da inserire nella pagina. */
        element() {
            return this.tableEl;
        }
        /* Costruisce la tabella a partire dai dati del progetto. */
        build() {
            var project = this.project;
            var table = dom.el("table");

            var head = dom.el("tr");
            head.appendChild(dom.el("th", { text: "WP / Task" }));
            dom.each(project.months, function(m) {
                head.appendChild(dom.el("th", { text: "M" + m }));
            });
            table.appendChild(head);

            dom.each(project.wps, function(wp) {
                var wpRow = dom.el("tr", { "class": "wp-row" });
                var wpCell = dom.el("td", { text: wp.label });
                wpCell.setAttribute("colspan", String(project.months.length + 1));
                wpRow.appendChild(wpCell);
                table.appendChild(wpRow);

                dom.each(wp.tasks, function(task) {
                    var tr = dom.el("tr");
                    tr.appendChild(dom.el("td", { "class": "task-label", text: task.label }));
                    dom.each(task.cells, function(cell) {
                        var td;
                        if (cell.editable) {
                            td = dom.el("td", {
                                "class": "editable",
                                text: (cell.worked === null || cell.worked === undefined) ? "0" : String(cell.worked)
                            });
                            // Coordinate della cella, per il salvataggio puntuale.
                            td.setAttribute("data-wp", String(task.wp));
                            td.setAttribute("data-task", String(task.task));
                            td.setAttribute("data-month", String(cell.month));
                        } else {
                            td = dom.el("td", {
                                "class": "noedit",
                                text: (cell.worked === null || cell.worked === undefined) ? "" : String(cell.worked)
                            });
                        }
                        tr.appendChild(td);
                    });
                    table.appendChild(tr);
                });
            });

            return table;
        }
        handleEvent(event) {
            if (event.type === "click") {
                var td = event.target.closest ? event.target.closest("td.editable") : null;
                if (td && this.tableEl.contains(td) && !this.editing) {
                    this.startEdit(td);
                }
            } else if (event.type === "blur") {
                this.commitEdit();
            } else if (event.type === "keydown") {
                if (event.key === "Enter") {
                    event.preventDefault();
                    this.editing.input.blur(); // provoca commitEdit tramite blur
                } else if (event.key === "Escape") {
                    this.cancelEdit();
                }
            }
        }
        /* Sostituisce il contenuto della cella con un input numerico. */
        startEdit(td) {
            var prev = td.textContent;
            var input = dom.el("input", { type: "number", min: "0", step: "1" });
            input.value = prev;

            this.editing = {
                td: td,
                input: input,
                prev: prev,
                wp: td.getAttribute("data-wp"),
                task: td.getAttribute("data-task"),
                month: td.getAttribute("data-month")
            };

            dom.clear(td);
            td.appendChild(input);
            input.addEventListener("blur", this);
            input.addEventListener("keydown", this);
            input.focus();
            input.select();
        }
        /* Annulla l'editing ripristinando il valore precedente. */
        cancelEdit() {
            if (!this.editing) {
                return;
            }
            var td = this.editing.td;
            var prev = this.editing.prev;
            this.editing = null;
            dom.clear(td);
            td.textContent = prev;
        }
        /* Conferma l'editing: valida, salva sul server, aggiorna o ripristina. */
        commitEdit() {
            if (!this.editing) {
                return;
            }
            var edit = this.editing;
            this.editing = null; // evita rientri durante la chiamata asincrona

            var raw = edit.input.value.trim();

            // Validazione lato client: intero non negativo (come sul server).
            if (!/^\d+$/.test(raw)) {
                dom.clear(edit.td);
                edit.td.textContent = edit.prev;
                if (this.onError) {
                    this.onError("Le ore lavorate devono essere un numero intero non negativo.");
                }
                return;
            }

            // Nessuna modifica: nessuna chiamata, si ripristina il testo.
            if (raw === String(edit.prev).trim()) {
                dom.clear(edit.td);
                edit.td.textContent = raw;
                return;
            }

            // Feedback provvisorio mentre si salva.
            dom.clear(edit.td);
            edit.td.textContent = raw;

            var self = this;
            this.api.saveHours(this.project.title, edit.wp, edit.task, edit.month, raw,
                function(result) {
                    if (result && result.success) {
                        edit.td.textContent = String(result.worked);
                        if (self.onError) {
                            self.onError(""); // pulisce eventuali errori precedenti
                        }
                    } else {
                        edit.td.textContent = edit.prev;
                        if (self.onError) {
                            self.onError((result && result.error) || "Salvataggio non riuscito.");
                        }
                    }
                },
                function(message) {
                    edit.td.textContent = edit.prev;
                    if (self.onError) {
                        self.onError(message);
                    }
                });
        }
    }







    /* ====================================================================== *
     *  CollaboratorApp - orchestratore della pagina                            *
     * ====================================================================== */
    class CollaboratorApp {
        constructor(ctx) {
            this.api = new CollaboratorApi(ctx);

            this.greetingEl = document.getElementById("greeting");
            this.errorEl = document.getElementById("errorMsg");
            this.listEl = document.getElementById("projectList");
            this.viewEl = document.getElementById("projectView");

            this.board = null;
            this.linkProject = []; // parallela ai link: link -> progetto
            this.activeLink = null;
            this.currentTable = null;

            this.load();
        }
        showError(message) {
            this.errorEl.textContent = message || "";
        }
        load() {
            var self = this;
            this.api.loadBoard(function(board) {
                self.onBoard(board);
            }, function(message) {
                self.showError(message);
            });
        }
        onBoard(board) {
            this.board = board;
            this.greetingEl.textContent = "Benvenuto, " + board.collaboratorName;
            this.showError("");

            dom.clear(this.listEl);
            this.linkProject = [];

            if (!board.projects || board.projects.length === 0) {
                this.listEl.appendChild(dom.el("li", {
                    "class": "hint", text: "Non sei assegnato ad alcun progetto."
                }));
                return;
            }

            var self = this;
            dom.each(board.projects, function(project) {
                var link = dom.el("a", { href: "#", text: project.title });
                link.addEventListener("click", self);
                self.linkProject.push({ link: link, project: project });

                var li = dom.el("li", null, [link]);
                self.listEl.appendChild(li);
            });
        }
        /* Listener dei link di progetto (handleEvent). */
        handleEvent(event) {
            event.preventDefault();
            var target = event.currentTarget;
            for (var i = 0;i < this.linkProject.length;i++) {
                if (this.linkProject[i].link === target) {
                    this.selectProject(this.linkProject[i].project, target);
                    return;
                }
            }
        }
        selectProject(project, link) {
            // Evidenzia il progetto selezionato.
            if (this.activeLink) {
                this.activeLink.className = "";
            }
            link.className = "active";
            this.activeLink = link;

            this.showError("");

            // Costruisce la tabella lato client, senza nuove richieste al server.
            dom.clear(this.viewEl);
            var self = this;
            this.currentTable = new ProjectTable(this.api, project, function(message) {
                self.showError(message);
            });
            this.viewEl.appendChild(dom.el("h2", { text: "Progetto: " + project.title }));
            this.viewEl.appendChild(this.currentTable.element());
        }
    }






    /* ----------------------------- avvio ---------------------------------- */
    new CollaboratorApp(window.APP_CONTEXT || "/");

})();