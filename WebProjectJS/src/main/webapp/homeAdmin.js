"use strict";

/*
 * =============================================================================
 *  HOME AMMINISTRATORE  (homeAdmin.html) - interfaccia single page (RIA)
 * =============================================================================
 *
 *  Comportamento (specifica, versione JavaScript):
 *
 *   - La creazione completa di un progetto con i suoi WP e task avviene INTERAMENTE
 *     lato client, in memoria. Solo alla pressione di SALVA l'intera struttura viene
 *     inviata al server per la registrazione permanente. Un progetto senza WP, o un
 *     WP senza task, fa fallire il salvataggio ma NON perde i dati inseriti.
 *   - Dopo aver creato il progetto tramite form, WP e task si aggiungono con il
 *     bottone "+"; un WP nuovo ha titolo provvisorio e durata pari al progetto, un
 *     task nuovo ha titolo provvisorio e durata pari al WP. Cliccando sul valore di
 *     un attributo lo si modifica. La numerazione di WP e task e' automatica.
 *   - Il bottone "-" elimina un WP (con i suoi task in cascata) o un task, con
 *     rinumerazione automatica dei successivi.
 *   - VERIFICA PROGETTI mostra la struttura gerarchica (WP -> task con mese inizio,
 *     mese fine, totale ore previste e lavorate); per default il progetto in
 *     costruzione, e un elenco selezionabile dei progetti gia' creati dall'admin.
 *
 *  Funzioni OPZIONALI implementate:
 *   - UNDO / REDO di tutte le operazioni lato client (pattern memento: si conserva
 *     uno snapshot dello stato prima di ogni modifica).
 *   - Spostamento dei task tra WP mediante DRAG & DROP, con controllo di congruenza
 *     tra la durata del task e quella del WP di destinazione.
 *
 *  Architettura (coerente con manager.js / collaborator.js):
 *   - unico oggetto globale TIW; utilita' in TIW.dom, AJAX in TIW.Http (utils.js);
 *   - componenti come "classi" (funzioni costruttore + prototype + this);
 *   - i controller sono essi stessi i listener degli eventi (handleEvent).
 *
 *  -------------------------------------------------------------------------
 *  CONTRATTO CON IL SERVER (endpoint ancora da implementare lato Java):
 *
 *    GET  {ctx}GetAdminData?resource=managers
 *         -> [ { "id": int, "nome": str, "cognome": str }, ... ]
 *            (personale tecnico assegnabile come responsabile)
 *
 *    GET  {ctx}GetAdminData?resource=projects
 *         -> [ { "titolo": str, "stato": str }, ... ]
 *            (progetti creati DA QUESTO amministratore)
 *
 *    GET  {ctx}GetAdminData?resource=projectStructure&progetto=<titolo>
 *         -> { "title": str,
 *              "wps": [ { "label": str, "meseInizio": int, "meseFine": int,
 *                         "tasks": [ { "label": str, "meseInizio": int,
 *                                      "meseFine": int, "orePreviste": int,
 *                                      "oreLavorate": int } ] } ] }
 *
 *    POST {ctx}AdminActionJS      (Content-Type: application/json)
 *         body: { "action": "saveProject",
 *                 "titolo": str, "durata": int, "responsabile": int,
 *                 "wps": [ { "titolo": str, "meseInizio": int, "meseFine": int,
 *                            "tasks": [ { "titolo": str, "meseInizio": int,
 *                                         "meseFine": int } ] } ] }
 *         -> { "success": true } | { "success": false, "error": str }
 *                                 | { "success": false, "problems": [str, ...] }
 *  -------------------------------------------------------------------------
 * =============================================================================
 */

(function () {

    var dom = TIW.dom;

    /* Clonazione profonda di dati JSON-izzabili (per gli snapshot di undo/redo). */
    function deepClone(value) {
        return value === null || value === undefined ? value : JSON.parse(JSON.stringify(value));
    }

    /* Parsing di un intero "stretto" (solo cifre); ritorna null se non valido. */
    function parseStrictInt(text) {
        var s = String(text).trim();
        return /^\d+$/.test(s) ? parseInt(s, 10) : null;
    }

    /* ====================================================================== *
     *  AdminApi - chiamate al server                                           *
     * ====================================================================== */
    class AdminApi {
        constructor(ctx) {
            this.ctx = ctx || "/";
            this.loginUrl = this.ctx + "login";
            this.http = new TIW.Http();
        }
        urlFor(resource, params) {
            var url = this.ctx + "GetAdminData?resource=" + encodeURIComponent(resource);
            if (params) {
                var keys = Object.keys(params);
                for (var i = 0;i < keys.length;i++) {
                    url += "&" + encodeURIComponent(keys[i]) + "=" + encodeURIComponent(params[keys[i]]);
                }
            }
            return url;
        }
        get(resource, params, onOk, onErr) {
            var self = this;
            this.http.request({
                method: "GET",
                url: this.urlFor(resource, params),
                headers: { "Accept": "application/json" },
                onUnauthorized: function() { window.location.href = self.loginUrl; },
                onSuccess: function(data) { onOk(data); },
                onError: function(message) { if (onErr) { onErr(message); } }
            });
        }
        /* Persistenza dell'intero progetto (JSON). */
        saveProject(project, onOk, onErr) {
            var self = this;
            var payload = { action: "saveProject" };
            payload.titolo = project.titolo;
            payload.durata = project.durata;
            payload.responsabile = project.responsabileId;
            payload.wps = project.wps.map(function(wp) {
                return {
                    titolo: wp.titolo,
                    meseInizio: wp.meseInizio,
                    meseFine: wp.meseFine,
                    tasks: wp.tasks.map(function(t) {
                        return { titolo: t.titolo, meseInizio: t.meseInizio, meseFine: t.meseFine };
                    })
                };
            });
            this.http.request({
                method: "POST",
                url: this.ctx + "AdminActionJS",
                headers: { "Content-Type": "application/json;charset=UTF-8" },
                body: JSON.stringify(payload),
                onUnauthorized: function() { window.location.href = self.loginUrl; },
                onSuccess: function(data) { onOk(data); },
                onError: function(message) { if (onErr) { onErr(message); } }
            });
        }
    }




    /* ====================================================================== *
     *  History - undo/redo mediante snapshot (memento)                         *
     * ====================================================================== */
    class History {
        constructor() {
            this.undoStack = [];
            this.redoStack = [];
        }
        reset() {
            this.undoStack = [];
            this.redoStack = [];
        }
        canUndo() { return this.undoStack.length > 0; }
        canRedo() { return this.redoStack.length > 0; }
        /* Da chiamare PRIMA di ogni modifica, passando lo stato corrente. */
        record(currentSnapshot) {
            this.undoStack.push(currentSnapshot);
            this.redoStack = [];
        }
        undo(currentSnapshot) {
            if (!this.canUndo()) { return null; }
            this.redoStack.push(currentSnapshot);
            return this.undoStack.pop();
        }
        redo(currentSnapshot) {
            if (!this.canRedo()) { return null; }
            this.undoStack.push(currentSnapshot);
            return this.redoStack.pop();
        }
    }

    /* ====================================================================== *
     *  BuilderView - VISTA "HOME AMMINISTRATORE": costruzione del progetto     *
     * ====================================================================== */
    class BuilderView {
        constructor(app) {
            this.app = app;
            this.api = app.api;

            this.model = { project: null }; // stato lato client (JSON-izzabile)
            this.history = new History();
            this.wpSeq = 0; // contatori id monotoni (fuori snapshot)
            this.taskSeq = 0;
            this.editing = null; // editing inline in corso
            this.drag = null; // task trascinato


            /* form di creazione progetto */
            this.formEl = document.getElementById("np-form");
            this.titoloInput = document.getElementById("np-titolo");
            this.durataInput = document.getElementById("np-durata");
            this.respSelect = document.getElementById("np-responsabile");
            this.createBtn = document.getElementById("btn-create-project");

            /* toolbar + contenitore albero */
            this.toolbarEl = document.getElementById("builder-toolbar");
            this.undoBtn = document.getElementById("btn-undo");
            this.redoBtn = document.getElementById("btn-redo");
            this.saveBtn = document.getElementById("btn-save-all");
            this.resetBtn = document.getElementById("btn-reset");
            this.treeEl = document.getElementById("admin-tree");
            this.msgEl = document.getElementById("admin-msg");

            /* listener sui controlli fissi (l'oggetto stesso e' il listener) */
            this.createBtn.addEventListener("click", this);
            this.undoBtn.addEventListener("click", this);
            this.redoBtn.addEventListener("click", this);
            this.saveBtn.addEventListener("click", this);
            this.resetBtn.addEventListener("click", this);

            /* delega degli eventi dell'albero (bottoni +/-, editing, drag&drop) */
            this.treeEl.addEventListener("click", this);
            this.treeEl.addEventListener("dragstart", this);
            this.treeEl.addEventListener("dragover", this);
            this.treeEl.addEventListener("drop", this);
            this.treeEl.addEventListener("dragend", this);

            this.render();
        }
        /* Dispatcher unico degli eventi. */
        handleEvent(event) {
            if (event.type === "click") {
                return this.onClick(event);
            }
            if (event.type === "dragstart") { return this.onDragStart(event); }
            if (event.type === "dragover") { return this.onDragOver(event); }
            if (event.type === "drop") { return this.onDrop(event); }
            if (event.type === "dragend") { this.drag = null; return; }
            if (event.type === "blur") { return this.commitEdit(false); }
            if (event.type === "keydown") {
                if (event.key === "Enter") { event.preventDefault(); this.commitEdit(true); }
                else if (event.key === "Escape") { this.cancelEdit(); }
            }
        }
        /* --- snapshot / undo / redo --- */
        snapshot() {
            return deepClone(this.model);
        }
        mutate(fn) {
            this.history.record(this.snapshot());
            fn();
            this.render();
        }
        onUndo() {
            var restored = this.history.undo(this.snapshot());
            if (restored) { this.model = restored; this.render(); }
        }
        onRedo() {
            var restored = this.history.redo(this.snapshot());
            if (restored) { this.model = restored; this.render(); }
        }
        showMsg(text, isError) {
            dom.showMsg(this.msgEl, text, isError);
        }
        /* --- gestione click (controlli fissi + delega albero) --- */
        onClick(event) {
            var target = event.currentTarget;
            if (target === this.createBtn) { return this.onCreateProject(); }
            if (target === this.undoBtn) { return this.onUndo(); }
            if (target === this.redoBtn) { return this.onRedo(); }
            if (target === this.saveBtn) { return this.onSaveAll(); }
            if (target === this.resetBtn) { return this.onReset(); }

            /* delega: bottoni con data-action nell'albero */
            var actionEl = event.target.closest ? event.target.closest("[data-action]") : null;
            if (actionEl && this.treeEl.contains(actionEl)) {
                return this.onTreeAction(actionEl);
            }
            /* delega: campi editabili inline */
            var editEl = event.target.closest ? event.target.closest(".js-edit") : null;
            if (editEl && this.treeEl.contains(editEl) && !this.editing) {
                return this.startEdit(editEl);
            }
        }
        onTreeAction(el) {
            var action = el.getAttribute("data-action");
            var wpId = el.getAttribute("data-wp");
            var taskId = el.getAttribute("data-task");
            if (action === "add-wp") { return this.addWp(); }
            if (action === "add-task") { return this.addTask(parseInt(wpId, 10)); }
            if (action === "del-wp") { return this.deleteWp(parseInt(wpId, 10)); }
            if (action === "del-task") { return this.deleteTask(parseInt(wpId, 10), parseInt(taskId, 10)); }
        }
        /* --- creazione / reset del progetto --- */
        onCreateProject() {
            var titolo = this.titoloInput.value.trim();
            var durata = parseStrictInt(this.durataInput.value);
            var responsabileId = this.respSelect.value ? parseInt(this.respSelect.value, 10) : null;

            if (!titolo) { return this.showMsg("Inserisci il titolo del progetto.", true); }
            if (durata === null || durata < 1) { return this.showMsg("La durata deve essere un intero positivo.", true); }
            if (!responsabileId) { return this.showMsg("Seleziona il responsabile del progetto.", true); }

            var respLabel = this.respSelect.options[this.respSelect.selectedIndex].textContent;
            var self = this;
            this.mutate(function() {
                self.model.project = {
                    titolo: titolo,
                    durata: durata,
                    responsabileId: responsabileId,
                    responsabileLabel: respLabel,
                    wps: []
                };
            });
            this.showMsg("Progetto creato lato client. Aggiungi WP e task, poi premi SALVA.", false);
        }
        onReset() {
            if (!this.model.project) { return; }
            var self = this;
            this.mutate(function() { self.model.project = null; });
            this.showMsg("", false);
        }
        /* --- operazioni su WP e task (tutte tracciate per undo/redo) --- */
        addWp() {
            if (!this.model.project) { return; }
            var self = this;
            var id = ++this.wpSeq;
            this.mutate(function() {
                self.model.project.wps.push({
                    id: id,
                    titolo: "Titolo del WP",
                    meseInizio: 1,
                    meseFine: self.model.project.durata,
                    tasks: []
                });
            });
        }
        addTask(wpId) {
            var wp = this.findWp(wpId);
            if (!wp) { return; }
            var self = this;
            var id = ++this.taskSeq;
            this.mutate(function() {
                wp.tasks.push({
                    id: id,
                    titolo: "Titolo del task",
                    meseInizio: wp.meseInizio,
                    meseFine: wp.meseFine
                });
            });
        }
        deleteWp(wpId) {
            var self = this;
            this.mutate(function() {
                self.model.project.wps = self.model.project.wps.filter(function(wp) { return wp.id !== wpId; });
            });
        }
        deleteTask(wpId, taskId) {
            var wp = this.findWp(wpId);
            if (!wp) { return; }
            this.mutate(function() {
                wp.tasks = wp.tasks.filter(function(t) { return t.id !== taskId; });
            });
        }
        findWp(wpId) {
            if (!this.model.project) { return null; }
            var wps = this.model.project.wps;
            for (var i = 0;i < wps.length;i++) {
                if (wps[i].id === wpId) { return wps[i]; }
            }
            return null;
        }
        findTask(wp, taskId) {
            for (var i = 0;i < wp.tasks.length;i++) {
                if (wp.tasks[i].id === taskId) { return wp.tasks[i]; }
            }
            return null;
        }
        /* --- editing inline di un attributo --- */
        startEdit(el) {
            var field = el.getAttribute("data-edit");
            var wpId = el.getAttribute("data-wp");
            var taskId = el.getAttribute("data-task");

            var control;
            if (field === "project.responsabile") {
                control = this.buildManagerSelect(this.model.project.responsabileId);
            } else if (field === "project.titolo" || field === "wp.titolo" || field === "task.titolo") {
                control = dom.el("input", { type: "text" });
                control.value = el.textContent;
            } else {
                control = dom.el("input", { type: "number", min: "1", step: "1" });
                control.value = el.textContent;
            }
            control.className = "edit-control";

            this.editing = {
                el: el,
                control: control,
                field: field,
                wpId: wpId ? parseInt(wpId, 10) : null,
                taskId: taskId ? parseInt(taskId, 10) : null,
                prev: el.textContent
            };

            el.replaceWith(control);
            control.addEventListener("blur", this);
            control.addEventListener("keydown", this);
            control.focus();
            if (control.select) { control.select(); }
        }
        cancelEdit() {
            if (!this.editing) { return; }
            this.editing = null;
            this.render();
        }
        commitEdit(fromEnter) {
            if (!this.editing) { return; }
            var edit = this.editing;
            this.editing = null;

            var project = this.model.project;
            var wp = edit.wpId ? this.findWp(edit.wpId) : null;
            var task = (wp && edit.taskId) ? this.findTask(wp, edit.taskId) : null;

            var raw = edit.control.value;
            var error = null;
            var self = this;
            var apply = null;

            switch (edit.field) {
                case "project.titolo":
                    var pt = raw.trim();
                    if (!pt) { error = "Il titolo non puo' essere vuoto."; }
                    else { apply = function() { project.titolo = pt; }; }
                    break;
                case "project.durata":
                    var d = parseStrictInt(raw);
                    var maxFine = this.maxWpFine();
                    if (d === null || d < 1) { error = "La durata deve essere un intero positivo."; }
                    else if (d < maxFine) { error = "La durata non puo' essere inferiore al mese di fine di un WP (" + maxFine + ")."; }
                    else { apply = function() { project.durata = d; }; }
                    break;
                case "project.responsabile":
                    var rid = edit.control.value ? parseInt(edit.control.value, 10) : null;
                    if (!rid) { error = "Seleziona un responsabile."; }
                    else {
                        var rlabel = edit.control.options[edit.control.selectedIndex].textContent;
                        apply = function() { project.responsabileId = rid; project.responsabileLabel = rlabel; };
                    }
                    break;
                case "wp.titolo":
                    var wt = raw.trim();
                    if (!wt) { error = "Il titolo del WP non puo' essere vuoto."; }
                    else { apply = function() { wp.titolo = wt; }; }
                    break;
                case "wp.meseInizio":
                case "wp.meseFine":
                    error = this.validateWpMonth(wp, edit.field, raw, function(val, which) {
                        apply = function() { wp[which] = val; };
                    });
                    break;
                case "task.titolo":
                    var tt = raw.trim();
                    if (!tt) { error = "Il titolo del task non puo' essere vuoto."; }
                    else { apply = function() { task.titolo = tt; }; }
                    break;
                case "task.meseInizio":
                case "task.meseFine":
                    error = this.validateTaskMonth(wp, task, edit.field, raw, function(val, which) {
                        apply = function() { task[which] = val; };
                    });
                    break;
            }

            if (error) {
                this.showMsg(error, true);
                this.render(); // ripristina il valore precedente
                return;
            }
            this.showMsg("", false);
            this.mutate(apply);
        }
        maxWpFine() {
            var max = 1;
            this.model.project.wps.forEach(function(wp) { if (wp.meseFine > max) { max = wp.meseFine; } });
            return max;
        }
        /* Ritorna un messaggio d'errore (o null) e, se valido, invoca setter(val, campo). */
        validateWpMonth(wp, field, raw, setter) {
            var val = parseStrictInt(raw);
            if (val === null || val < 1) { return "Il mese deve essere un intero positivo."; }
            if (val > this.model.project.durata) { return "Il mese deve essere entro la durata del progetto (" + this.model.project.durata + ")."; }
            var inizio = (field === "wp.meseInizio") ? val : wp.meseInizio;
            var fine = (field === "wp.meseFine") ? val : wp.meseFine;
            if (inizio > fine) { return "Il mese di inizio non puo' superare quello di fine."; }
            /* i task del WP devono restare contenuti nel nuovo intervallo */
            for (var i = 0;i < wp.tasks.length;i++) {
                if (wp.tasks[i].meseInizio < inizio || wp.tasks[i].meseFine > fine) {
                    return "L'intervallo del WP non contiene piu' il task \"" + wp.tasks[i].titolo + "\".";
                }
            }
            setter(val, field === "wp.meseInizio" ? "meseInizio" : "meseFine");
            return null;
        }
        validateTaskMonth(wp, task, field, raw, setter) {
            var val = parseStrictInt(raw);
            if (val === null || val < 1) { return "Il mese deve essere un intero positivo."; }
            var inizio = (field === "task.meseInizio") ? val : task.meseInizio;
            var fine = (field === "task.meseFine") ? val : task.meseFine;
            if (inizio > fine) { return "Il mese di inizio non puo' superare quello di fine."; }
            if (inizio < wp.meseInizio || fine > wp.meseFine) {
                return "Il task deve stare nell'intervallo del WP (" + wp.meseInizio + "-" + wp.meseFine + ").";
            }
            setter(val, field === "task.meseInizio" ? "meseInizio" : "meseFine");
            return null;
        }
        /* --- drag & drop dei task tra WP --- */
        onDragStart(event) {
            var li = event.target.closest ? event.target.closest("[data-drag-task]") : null;
            if (!li) { return; }
            this.drag = {
                wpId: parseInt(li.getAttribute("data-wp"), 10),
                taskId: parseInt(li.getAttribute("data-task"), 10)
            };
            if (event.dataTransfer) {
                event.dataTransfer.effectAllowed = "move";
                try { event.dataTransfer.setData("text/plain", String(this.drag.taskId)); } catch (e) { /* IE */ }
            }
        }
        onDragOver(event) {
            if (!this.drag) { return; }
            var zone = event.target.closest ? event.target.closest("[data-drop-wp]") : null;
            if (zone) {
                event.preventDefault(); // necessario per abilitare il drop
                if (event.dataTransfer) { event.dataTransfer.dropEffect = "move"; }
            }
        }
        onDrop(event) {
            if (!this.drag) { return; }
            var zone = event.target.closest ? event.target.closest("[data-drop-wp]") : null;
            if (!zone) { return; }
            event.preventDefault();

            var destWpId = parseInt(zone.getAttribute("data-drop-wp"), 10);
            var beforeLi = event.target.closest ? event.target.closest("[data-drag-task]") : null;
            var beforeTaskId = beforeLi ? parseInt(beforeLi.getAttribute("data-task"), 10) : null;
            this.moveTask(this.drag.wpId, this.drag.taskId, destWpId, beforeTaskId);
            this.drag = null;
        }
        moveTask(srcWpId, taskId, destWpId, beforeTaskId) {
            if (beforeTaskId === taskId) { return; } // rilascio su se stesso
            var srcWp = this.findWp(srcWpId);
            var destWp = this.findWp(destWpId);
            if (!srcWp || !destWp) { return; }
            var task = this.findTask(srcWp, taskId);
            if (!task) { return; }

            /* congruenza: il task deve stare nell'intervallo del WP di destinazione */
            if (task.meseInizio < destWp.meseInizio || task.meseFine > destWp.meseFine) {
                this.showMsg("Spostamento rifiutato: i mesi del task (" + task.meseInizio + "-" + task.meseFine +
                    ") non rientrano nel WP di destinazione (" + destWp.meseInizio + "-" + destWp.meseFine + ").", true);
                return;
            }

            var self = this;
            this.mutate(function() {
                srcWp.tasks = srcWp.tasks.filter(function(t) { return t.id !== taskId; });
                var index = destWp.tasks.length;
                if (beforeTaskId !== null) {
                    for (var i = 0;i < destWp.tasks.length;i++) {
                        if (destWp.tasks[i].id === beforeTaskId) { index = i; break; }
                    }
                }
                destWp.tasks.splice(index, 0, task);
            });
            this.showMsg("", false);
        }
        /* --- SALVA: validazione client + invio dell'intera struttura --- */
        onSaveAll() {
            var project = this.model.project;
            if (!project) { return this.showMsg("Nessun progetto da salvare.", true); }
            if (project.wps.length === 0) { return this.showMsg("Il progetto deve contenere almeno un WP.", true); }
            for (var i = 0;i < project.wps.length;i++) {
                if (project.wps[i].tasks.length === 0) {
                    return this.showMsg("Ogni WP deve contenere almeno un task (WP" + (i + 1) + ").", true);
                }
            }

            var self = this;
            this.saveBtn.disabled = true;
            this.api.saveProject(project, function(result) {
                self.saveBtn.disabled = false;
                if (result && result.success) {
                    self.showMsg("Progetto salvato con successo.", false);
                    self.mutate(function() { self.model.project = null; });
                    self.app.notifyProjectsChanged();
                } else if (result && result.problems && result.problems.length > 0) {
                    self.showMsg("Salvataggio non riuscito: " + result.problems.join(" | "), true);
                } else {
                    self.showMsg((result && result.error) || "Salvataggio non riuscito.", true);
                }
            }, function(message) {
                self.saveBtn.disabled = false;
                self.showMsg(message, true);
            });
        }
        /* --- costruzione della <select> dei responsabili --- */
        buildManagerSelect(selectedId) {
            var select = dom.el("select");
            select.appendChild(dom.el("option", { value: "", text: "-- responsabile --" }));
            this.app.managers.forEach(function(m) {
                var opt = dom.el("option", { value: String(m.id), text: m.nome + " " + m.cognome });
                if (selectedId && m.id === selectedId) { opt.selected = true; }
                select.appendChild(opt);
            });
            return select;
        }
        /* --- rendering completo dell'albero del progetto --- */
        render() {
            /* aggiorna abilitazione dei bottoni di toolbar */
            this.undoBtn.disabled = !this.history.canUndo();
            this.redoBtn.disabled = !this.history.canRedo();
            var hasProject = !!this.model.project;
            this.saveBtn.disabled = !hasProject;
            this.resetBtn.disabled = !hasProject;
            this.formEl.hidden = hasProject;
            this.toolbarEl.hidden = !hasProject;

            dom.clear(this.treeEl);
            if (!hasProject) { return; }

            var project = this.model.project;

            /* intestazione del progetto con attributi editabili */
            var header = dom.el("div", { "class": "proj-header" });
            header.appendChild(dom.el("strong", { text: "PROGETTO: " }));
            header.appendChild(this.editable(project.titolo, "project.titolo", null, null));
            header.appendChild(document.createTextNode("  -  durata: "));
            header.appendChild(this.editable(String(project.durata), "project.durata", null, null));
            header.appendChild(document.createTextNode(" mesi  -  responsabile: "));
            header.appendChild(this.editable(project.responsabileLabel, "project.responsabile", null, null));
            header.appendChild(document.createTextNode(" "));
            header.appendChild(this.actionButton("+ WP", "add-wp", null, null));
            this.treeEl.appendChild(header);

            if (project.wps.length === 0) {
                this.treeEl.appendChild(dom.el("p", { "class": "muted", text: "Nessun WP: usa \"+ WP\" per aggiungerne uno." }));
                return;
            }

            var wpList = dom.el("ul", { "class": "wp-list" });
            var self = this;
            project.wps.forEach(function(wp, wi) {
                var wpLi = dom.el("li", { "class": "wp-item" });
                wpLi.setAttribute("data-drop-wp", String(wp.id)); // zona di rilascio drag&drop

                var wpRow = dom.el("div", { "class": "wp-row" });
                wpRow.appendChild(dom.el("strong", { text: "WP" + (wi + 1) + ": " }));
                wpRow.appendChild(self.editable(wp.titolo, "wp.titolo", wp.id, null));
                wpRow.appendChild(document.createTextNode("  mesi "));
                wpRow.appendChild(self.editable(String(wp.meseInizio), "wp.meseInizio", wp.id, null));
                wpRow.appendChild(document.createTextNode(" - "));
                wpRow.appendChild(self.editable(String(wp.meseFine), "wp.meseFine", wp.id, null));
                wpRow.appendChild(document.createTextNode(" "));
                wpRow.appendChild(self.actionButton("+ task", "add-task", wp.id, null));
                wpRow.appendChild(self.actionButton("-", "del-wp", wp.id, null, "btn-del"));
                wpLi.appendChild(wpRow);

                var taskList = dom.el("ul", { "class": "task-list" });
                wp.tasks.forEach(function(task, ti) {
                    var taskLi = dom.el("li", { "class": "task-item" });
                    taskLi.setAttribute("draggable", "true");
                    taskLi.setAttribute("data-drag-task", "1");
                    taskLi.setAttribute("data-wp", String(wp.id));
                    taskLi.setAttribute("data-task", String(task.id));

                    taskLi.appendChild(dom.el("span", { "class": "grip", text: "☰ " }));
                    taskLi.appendChild(dom.el("span", { text: "T" + (wi + 1) + "." + (ti + 1) + ": " }));
                    taskLi.appendChild(self.editable(task.titolo, "task.titolo", wp.id, task.id));
                    taskLi.appendChild(document.createTextNode("  mesi "));
                    taskLi.appendChild(self.editable(String(task.meseInizio), "task.meseInizio", wp.id, task.id));
                    taskLi.appendChild(document.createTextNode(" - "));
                    taskLi.appendChild(self.editable(String(task.meseFine), "task.meseFine", wp.id, task.id));
                    taskLi.appendChild(document.createTextNode(" "));
                    taskLi.appendChild(self.actionButton("-", "del-task", wp.id, task.id, "btn-del"));
                    taskList.appendChild(taskLi);
                });
                if (wp.tasks.length === 0) {
                    taskList.appendChild(dom.el("li", { "class": "muted", text: "Nessun task: usa \"+ task\"." }));
                }
                wpLi.appendChild(taskList);
                wpList.appendChild(wpLi);
            });
            this.treeEl.appendChild(wpList);
        }
        /* Crea uno <span> editabile (click -> editing inline). */
        editable(text, field, wpId, taskId) {
            var span = dom.el("span", { "class": "js-edit", text: text });
            span.setAttribute("data-edit", field);
            if (wpId !== null && wpId !== undefined) { span.setAttribute("data-wp", String(wpId)); }
            if (taskId !== null && taskId !== undefined) { span.setAttribute("data-task", String(taskId)); }
            return span;
        }
        /* Crea un bottone azione con i relativi data-attributi. */
        actionButton(label, action, wpId, taskId, cls) {
            var btn = dom.el("button", { type: "button", "class": cls ? ("tree-btn " + cls) : "tree-btn", text: label });
            btn.setAttribute("data-action", action);
            if (wpId !== null && wpId !== undefined) { btn.setAttribute("data-wp", String(wpId)); }
            if (taskId !== null && taskId !== undefined) { btn.setAttribute("data-task", String(taskId)); }
            return btn;
        }
        /* Snapshot dell'albero attuale per la vista VERIFICA (bozza in costruzione). */
        currentDraft() {
            return this.model.project ? deepClone(this.model.project) : null;
        }
    }





























    /* ====================================================================== *
     *  VerifyView - VISTA "VERIFICA PROGETTI"                                  *
     * ====================================================================== */
    class VerifyView {
        constructor(app) {
            this.app = app;
            this.api = app.api;
            this.loaded = false;

            this.select = document.getElementById("verify-select");
            this.draftEl = document.getElementById("verify-draft");
            this.structEl = document.getElementById("verify-struct");
            this.msgEl = document.getElementById("verify-msg");

            this.select.addEventListener("change", this);
        }
        handleEvent(event) {
            if (event.currentTarget === this.select) { this.onSelectChange(); }
        }
        /* Caricamento pigro dell'elenco progetti creati dall'admin. */
        ensureLoaded() {
            this.renderDraft();
            if (this.loaded) { return; }
            this.loaded = true;
            this.reloadProjects();
        }
        reloadProjects() {
            var self = this;
            this.api.get("projects", null, function(projects) {
                dom.fillSelect(self.select, projects.map(function(p) {
                    return { value: p.titolo, label: p.titolo + " (stato: " + p.stato + ")" };
                }), "Seleziona un progetto salvato...");
            }, function(message) {
                self.loaded = false;
                dom.showMsg(self.msgEl, message, true);
            });
        }
        /* Mostra la struttura del progetto in costruzione (bozza lato client). */
        renderDraft() {
            dom.clear(this.draftEl);
            var draft = this.app.builder.currentDraft();
            if (!draft) {
                this.draftEl.appendChild(dom.el("p", { "class": "muted", text: "Nessun progetto in costruzione." }));
                return;
            }
            this.draftEl.appendChild(dom.el("h3", { text: "Progetto in costruzione (non ancora salvato)" }));
            var view = {
                title: draft.titolo,
                wps: draft.wps.map(function(wp, wi) {
                    return {
                        label: "WP" + (wi + 1) + ": " + wp.titolo,
                        meseInizio: wp.meseInizio,
                        meseFine: wp.meseFine,
                        tasks: wp.tasks.map(function(t, ti) {
                            return {
                                label: "T" + (wi + 1) + "." + (ti + 1) + ": " + t.titolo,
                                meseInizio: t.meseInizio,
                                meseFine: t.meseFine,
                                orePreviste: 0,
                                oreLavorate: 0
                            };
                        })
                    };
                })
            };
            this.draftEl.appendChild(this.buildStructure(view));
        }
        onSelectChange() {
            dom.clear(this.structEl);
            dom.showMsg(this.msgEl, "");
            var titolo = this.select.value;
            if (!titolo) { return; }
            var self = this;
            this.api.get("projectStructure", { progetto: titolo }, function(data) {
                self.structEl.appendChild(dom.el("h3", { text: "Progetto salvato: " + data.title }));
                self.structEl.appendChild(self.buildStructure(data));
            }, function(message) {
                dom.showMsg(self.msgEl, message, true);
            });
        }
        /* Costruisce la lista gerarchica WP -> task con i totali. */
        buildStructure(data) {
            var root = dom.el("ul", { "class": "verify-tree" });
            if (!data.wps || data.wps.length === 0) {
                root.appendChild(dom.el("li", { "class": "muted", text: "Nessun WP." }));
                return root;
            }
            data.wps.forEach(function(wp) {
                var wpLi = dom.el("li", null, [
                    dom.el("strong", { text: wp.label + " (mesi " + wp.meseInizio + "-" + wp.meseFine + ")" })
                ]);
                var taskUl = dom.el("ul");
                if (!wp.tasks || wp.tasks.length === 0) {
                    taskUl.appendChild(dom.el("li", { "class": "muted", text: "Nessun task." }));
                } else {
                    wp.tasks.forEach(function(t) {
                        taskUl.appendChild(dom.el("li", {
                            text: t.label + " - mesi " + t.meseInizio + "-" + t.meseFine +
                                " - ore previste: " + t.orePreviste + " - ore lavorate: " + t.oreLavorate
                        }));
                    });
                }
                wpLi.appendChild(taskUl);
                root.appendChild(wpLi);
            });
            return root;
        }
    }







    /* ====================================================================== *
     *  AdminApp - orchestratore: navigazione, dati comuni, viste               *
     * ====================================================================== */
    class AdminApp {
        constructor(api) {
            this.api = api;
            this.managers = [];

            this.greetingEl = document.getElementById("greeting");
            this.navButtons = document.querySelectorAll("#mainNav .nav-btn");
            this.views = document.querySelectorAll(".view");

            var username = sessionStorage.getItem("username");
            if (username) { this.greetingEl.textContent = "Home Amministratore - " + username; }

            this.logout = new TIW.LogoutController("logoutBtn");

            var self = this;
            dom.each(this.navButtons, function(btn) { btn.addEventListener("click", self); });

            /* Carica una sola volta l'elenco dei responsabili, poi avvia le viste. */
            this.api.get("managers", null, function(managers) {
                self.managers = managers || [];
                self.populateManagerSelect();
                self.start();
            }, function(message) {
                /* Anche senza responsabili l'interfaccia parte: si mostra l'errore. */
                self.start();
                dom.showMsg(document.getElementById("admin-msg"), message, true);
            });
        }
        populateManagerSelect() {
            var select = document.getElementById("np-responsabile");
            dom.fillSelect(select, this.managers.map(function(m) {
                return { value: m.id, label: m.nome + " " + m.cognome };
            }), "-- responsabile --");
        }
        start() {
            this.builder = new BuilderView(this);
            this.verify = new VerifyView(this);
            this.showView("view-builder");
        }
        handleEvent(event) {
            this.showView(event.currentTarget.getAttribute("data-view"));
        }
        showView(id) {
            dom.each(this.views, function(v) { v.hidden = (v.id !== id); });
            dom.each(this.navButtons, function(b) {
                b.className = (b.getAttribute("data-view") === id) ? "nav-btn active" : "nav-btn";
            });
            if (id === "view-verify") { this.verify.ensureLoaded(); }
        }
        /* Chiamato dal builder dopo un salvataggio riuscito: aggiorna VERIFICA. */
        notifyProjectsChanged() {
            if (this.verify) { this.verify.loaded = false; }
        }
    }






    /* ----------------------------- avvio ---------------------------------- */
    new AdminApp(new AdminApi(window.APP_CONTEXT || "/"));

})();