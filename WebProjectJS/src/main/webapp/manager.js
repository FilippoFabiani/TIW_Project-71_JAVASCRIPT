"use strict";

/*
 * Interfaccia JavaScript del responsabile (single page), scritta in stile ES5
 * (var, function, XMLHttpRequest) per essere compatibile col validatore JS di
 * Eclipse, come loginManagement.js. Le richieste sono asincrone (XHR) e
 * aggiornano solo la porzione di pagina che cambia.
 *
 *   - letture: GET  <context>/GetManagerData?resource=...
 *   - azioni:  POST <context>/ManagerActionJS  (action=save|assign|conclude)
 */

var CTX = window.APP_CONTEXT || "/";
var LOGIN_URL = CTX + "login";

/* ----------------------- helper generici ----------------------- */

function each(list, fn) {
    for (var i = 0; i < list.length; i++) {
        fn(list[i], i);
    }
}

function encodeParams(pairs) {
    var parts = [];
    for (var i = 0; i < pairs.length; i++) {
        parts.push(encodeURIComponent(pairs[i][0]) + "=" + encodeURIComponent(pairs[i][1]));
    }
    return parts.join("&");
}

function apiUrl(resource, params) {
    var url = CTX + "GetManagerData?resource=" + encodeURIComponent(resource);
    if (params) {
        var keys = Object.keys(params);
        for (var i = 0; i < keys.length; i++) {
            url += "&" + encodeURIComponent(keys[i]) + "=" + encodeURIComponent(params[keys[i]]);
        }
    }
    return url;
}

function handleResponse(req, onOk, onError) {
    if (req.status === 401) {
        window.location.href = LOGIN_URL;
        return;
    }
    var data = null;
    try {
        data = req.responseText ? JSON.parse(req.responseText) : null;
    } catch (e) {
        data = null;
    }
    if (req.status >= 200 && req.status < 300) {
        onOk(data);
    } else if (onError) {
        onError((data && data.error) ? data.error : ("Errore " + req.status));
    }
}

/* GET verso GetManagerData */
function apiGet(resource, params, onOk, onError) {
    var req = new XMLHttpRequest();
    req.open("GET", apiUrl(resource, params), true);
    req.setRequestHeader("Accept", "application/json");
    req.onreadystatechange = function () {
        if (req.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        handleResponse(req, onOk, onError);
    };
    req.send();
}

/* POST verso ManagerActionJS; pairs = array di [chiave, valore] */
function apiAction(pairs, onOk, onError) {
    var req = new XMLHttpRequest();
    req.open("POST", CTX + "ManagerActionJS", true);
    req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
    req.onreadystatechange = function () {
        if (req.readyState !== XMLHttpRequest.DONE) {
            return;
        }
        handleResponse(req, onOk, onError);
    };
    req.send(encodeParams(pairs));
}

function el(tag, props, children) {
    var node = document.createElement(tag);
    if (props) {
        var keys = Object.keys(props);
        for (var i = 0; i < keys.length; i++) {
            var key = keys[i];
            if (key === "text") {
                node.textContent = props[key];
            } else if (key === "class") {
                node.className = props[key];
            } else {
                node.setAttribute(key, props[key]);
            }
        }
    }
    if (children) {
        for (var j = 0; j < children.length; j++) {
            node.appendChild(children[j]);
        }
    }
    return node;
}

function clear(node) {
    while (node.firstChild) {
        node.removeChild(node.firstChild);
    }
}

function fillSelect(select, options, placeholder) {
    clear(select);
    select.appendChild(el("option", { value: "", text: placeholder || "Seleziona..." }));
    for (var i = 0; i < options.length; i++) {
        select.appendChild(el("option", { value: String(options[i].value), text: options[i].label }));
    }
}

function resetSelect(select, disable) {
    clear(select);
    select.appendChild(el("option", { value: "", text: "--" }));
    if (disable) {
        select.disabled = true;
    }
}

function showMsg(node, text, isError) {
    node.textContent = text || "";
    node.className = "msg " + (isError ? "msg-err" : "msg-ok");
}

/* ============================================================== */
/*  NAVIGAZIONE TRA LE VISTE                                        */
/* ============================================================== */

each(document.querySelectorAll("#mainNav .nav-btn"), function (btn) {
    btn.addEventListener("click", function () {
        showView(btn.getAttribute("data-view"));
    });
});

function showView(id) {
    each(document.querySelectorAll(".view"), function (v) {
        v.hidden = (v.id !== id);
    });
    each(document.querySelectorAll("#mainNav .nav-btn"), function (b) {
        if (b.getAttribute("data-view") === id) {
            b.className = "nav-btn active";
        } else {
            b.className = "nav-btn";
        }
    });
    if (id === "view-home") {
        ensureHomeProjects();
    } else if (id === "view-projects") {
        ensureProjMonProjects();
    } else if (id === "view-collaborators") {
        ensureCollabList();
    }
}

/* ============================================================== */
/*  VISTA 1 - HOME RESPONSABILE                                     */
/* ============================================================== */

var homeProjectSel = document.getElementById("home-project");
var homeWpSel = document.getElementById("home-wp");
var homeTaskSel = document.getElementById("home-task");
var homeStatus = document.getElementById("home-status");
var homeDetail = document.getElementById("home-detail");
var homeActions = document.getElementById("home-actions");
var homeMsg = document.getElementById("home-msg");

var homeProjectsLoaded = false;
var homeState = { progetto: null, wp: null, task: null, months: [], editable: false };

function optionsFromProjects(projects) {
    return projects.map(function (p) {
        return { value: p.titolo, label: p.titolo + " (stato: " + p.stato + ")" };
    });
}

function ensureHomeProjects() {
    if (homeProjectsLoaded) {
        return;
    }
    homeProjectsLoaded = true;
    apiGet("projects", null, function (projects) {
        fillSelect(homeProjectSel, optionsFromProjects(projects));
    }, function (msg) {
        homeProjectsLoaded = false;
        showMsg(homeMsg, msg, true);
    });
}

homeProjectSel.addEventListener("change", function () {
    resetSelect(homeWpSel, true);
    resetSelect(homeTaskSel, true);
    clear(homeDetail);
    homeActions.hidden = true;
    homeStatus.textContent = "";
    showMsg(homeMsg, "");

    var progetto = homeProjectSel.value;
    homeState = { progetto: progetto || null, wp: null, task: null, months: [], editable: false };
    if (!progetto) {
        return;
    }

    apiGet("wps", { progetto: progetto }, function (wps) {
        fillSelect(homeWpSel, wps.map(function (w) { return { value: w.id, label: w.label }; }));
        homeWpSel.disabled = false;
    }, function (msg) {
        showMsg(homeMsg, msg, true);
    });
});

homeWpSel.addEventListener("change", function () {
    resetSelect(homeTaskSel, true);
    clear(homeDetail);
    homeActions.hidden = true;
    showMsg(homeMsg, "");

    homeState.wp = homeWpSel.value ? parseInt(homeWpSel.value, 10) : null;
    homeState.task = null;
    if (!homeState.wp) {
        return;
    }

    apiGet("tasks", { progetto: homeState.progetto, wp: homeState.wp }, function (tasks) {
        fillSelect(homeTaskSel, tasks.map(function (t) { return { value: t.id, label: t.label }; }));
        homeTaskSel.disabled = false;
    }, function (msg) {
        showMsg(homeMsg, msg, true);
    });
});

homeTaskSel.addEventListener("change", function () {
    clear(homeDetail);
    homeActions.hidden = true;
    showMsg(homeMsg, "");

    homeState.task = homeTaskSel.value ? parseInt(homeTaskSel.value, 10) : null;
    if (!homeState.task) {
        return;
    }

    apiGet("taskDetail", { progetto: homeState.progetto, wp: homeState.wp, task: homeState.task },
        function (detail) {
            renderTaskDetail(detail);
        }, function (msg) {
            showMsg(homeMsg, msg, true);
        });
});

function renderTaskDetail(detail) {
    homeState.months = detail.months || [];
    homeState.editable = !!detail.editable;
    homeStatus.textContent = "Stato del progetto: " + detail.status + (detail.editable ? "" : " (sola lettura)");

    if (homeState.months.length === 0) {
        homeDetail.appendChild(el("p", { "class": "muted", text: "Il task non ha un intervallo di mesi valido." }));
        return;
    }

    homeDetail.appendChild(el("h3", { text: "Ore previste (mese per mese)" }));
    var table = el("table", { "class": "grid" });
    var headRow = el("tr");
    var valRow = el("tr");
    each(homeState.months, function (m) {
        headRow.appendChild(el("th", { text: "M" + m }));
        var input = el("input", { type: "number", min: "0", id: "ore_" + m });
        var planned = detail.plannedHours ? detail.plannedHours[m] : undefined;
        if (planned !== undefined && planned !== null) {
            input.value = planned;
        }
        if (!detail.editable) {
            input.disabled = true;
        }
        valRow.appendChild(el("td", null, [input]));
    });
    table.appendChild(headRow);
    table.appendChild(valRow);
    homeDetail.appendChild(table);

    homeDetail.appendChild(el("h3", { text: "Collaboratori incaricati" }));
    var assigned = detail.assignedIds || [];
    if (!detail.collaborators || detail.collaborators.length === 0) {
        homeDetail.appendChild(el("p", { "class": "muted", text: "Nessun collaboratore disponibile." }));
    } else {
        each(detail.collaborators, function (c) {
            var cb = el("input", { type: "checkbox", value: String(c.id), "class": "collab-cb" });
            if (assigned.indexOf(c.id) !== -1) {
                cb.checked = true;
            }
            if (!detail.editable) {
                cb.disabled = true;
            }
            var label = el("label", null, [cb, document.createTextNode(" " + c.nome + " " + c.cognome)]);
            homeDetail.appendChild(label);
            homeDetail.appendChild(el("br"));
        });
    }

    homeActions.hidden = !detail.editable;
}

document.getElementById("btn-save").addEventListener("click", function () {
    if (!homeState.task) {
        return;
    }
    var pairs = [];
    each(homeState.months, function (m) {
        var input = document.getElementById("ore_" + m);
        if (input && input.value !== "") {
            pairs.push(["ore_" + m, input.value]);
        }
    });
    each(document.querySelectorAll("#home-detail .collab-cb"), function (cb) {
        if (cb.checked) {
            pairs.push(["collaboratore", cb.value]);
        }
    });
    pairs.push(["action", "save"]);
    pairs.push(["progetto", homeState.progetto]);
    pairs.push(["wp", homeState.wp]);
    pairs.push(["task", homeState.task]);

    apiAction(pairs, function (result) {
        if (result && result.success) {
            showMsg(homeMsg, "Dati salvati correttamente.", false);
        } else {
            showMsg(homeMsg, (result && result.error) || "Salvataggio non riuscito.", true);
        }
    }, function (msg) {
        showMsg(homeMsg, msg, true);
    });
});

document.getElementById("btn-assign").addEventListener("click", function () {
    if (!homeState.progetto) {
        return;
    }
    var pairs = [["action", "assign"], ["progetto", homeState.progetto]];
    apiAction(pairs, function (result) {
        if (result && result.success) {
            showMsg(homeMsg, "Progetto assegnato con successo.", false);
            // il progetto diventa 'assegnato' -> ricarico il dettaglio in sola lettura.
            // I dati non si perdono in caso di errore perche' restano nel DOM.
            if (homeState.task) {
                var evt = document.createEvent("HTMLEvents");
                evt.initEvent("change", true, true);
                homeTaskSel.dispatchEvent(evt);
            }
        } else if (result && result.problems && result.problems.length > 0) {
            showMsg(homeMsg, "Impossibile assegnare: " + result.problems.join(" | "), true);
        } else {
            showMsg(homeMsg, (result && result.error) || "Assegnamento non riuscito.", true);
        }
    }, function (msg) {
        showMsg(homeMsg, msg, true);
    });
});

/* ============================================================== */
/*  VISTA 2 - MONITORAGGIO PROGETTI                                 */
/* ============================================================== */

var projSel = document.getElementById("proj-select");
var projTable = document.getElementById("proj-table");
var projActions = document.getElementById("proj-actions");
var projMsg = document.getElementById("proj-msg");
var projProjectsLoaded = false;
var projCurrent = null;

function ensureProjMonProjects() {
    if (projProjectsLoaded) {
        return;
    }
    projProjectsLoaded = true;
    apiGet("projects", null, function (projects) {
        fillSelect(projSel, optionsFromProjects(projects));
    }, function (msg) {
        projProjectsLoaded = false;
        showMsg(projMsg, msg, true);
    });
}

projSel.addEventListener("change", function () {
    clear(projTable);
    projActions.hidden = true;
    showMsg(projMsg, "");
    projCurrent = projSel.value || null;
    if (!projCurrent) {
        return;
    }
    loadProjectMonitoring();
});

function loadProjectMonitoring() {
    apiGet("projectMonitoring", { progetto: projCurrent }, function (data) {
        renderProjectMonitoring(data);
    }, function (msg) {
        showMsg(projMsg, msg, true);
    });
}

function renderProjectMonitoring(data) {
    clear(projTable);

    if (!data.wpGroups || data.wpGroups.length === 0) {
        projTable.appendChild(el("p", { "class": "muted", text: "Il progetto non contiene WP o task." }));
        projActions.hidden = true;
        return;
    }

    var table = el("table", { "class": "grid" });
    var head = el("tr");
    head.appendChild(el("th", { text: "WP / Task" }));
    each(data.months, function (m) {
        head.appendChild(el("th", { text: "M" + m + " prev." }));
        head.appendChild(el("th", { text: "M" + m + " lav." }));
    });
    table.appendChild(head);

    each(data.wpGroups, function (wp) {
        var wpRow = el("tr", { "class": "wp-row" });
        var wpCell = el("td", { text: wp.label });
        wpCell.setAttribute("colspan", String(data.months.length * 2 + 1));
        wpRow.appendChild(wpCell);
        table.appendChild(wpRow);

        each(wp.tasks, function (task) {
            var tr = el("tr");
            tr.appendChild(el("td", { "class": "task-label", text: task.label }));
            each(task.cells, function (cell) {
                tr.appendChild(el("td", { text: String(cell.previste) }));
                tr.appendChild(el("td", { text: String(cell.lavorate) }));
            });
            table.appendChild(tr);
        });
    });
    projTable.appendChild(table);

    var canConclude = data.concludable && data.status && data.status.toLowerCase() === "assegnato";
    projActions.hidden = !canConclude;

    if (data.status && data.status.toLowerCase() === "concluso") {
        projTable.appendChild(el("p", { "class": "msg-ok", text: "Il progetto e' gia' concluso." }));
    }
}

document.getElementById("btn-conclude").addEventListener("click", function () {
    if (!projCurrent) {
        return;
    }
    var pairs = [["action", "conclude"], ["progetto", projCurrent]];
    apiAction(pairs, function (result) {
        if (result && result.success) {
            showMsg(projMsg, "Progetto concluso con successo.", false);
            loadProjectMonitoring();
        } else {
            showMsg(projMsg, (result && result.error) || "Conclusione non riuscita.", true);
        }
    }, function (msg) {
        showMsg(projMsg, msg, true);
    });
});

/* ============================================================== */
/*  VISTA 3 - MONITORAGGIO COLLABORATORI                           */
/* ============================================================== */

var collabSel = document.getElementById("collab-select");
var collabTables = document.getElementById("collab-tables");
var collabMsg = document.getElementById("collab-msg");
var collabListLoaded = false;

function ensureCollabList() {
    if (collabListLoaded) {
        return;
    }
    collabListLoaded = true;
    apiGet("collaborators", null, function (people) {
        fillSelect(collabSel, people.map(function (c) {
            return { value: c.id, label: c.nome + " " + c.cognome };
        }));
        if (people.length === 0) {
            showMsg(collabMsg, "Nessun collaboratore lavora nei tuoi progetti.", false);
        }
    }, function (msg) {
        collabListLoaded = false;
        showMsg(collabMsg, msg, true);
    });
}

collabSel.addEventListener("change", function () {
    clear(collabTables);
    showMsg(collabMsg, "");
    var id = collabSel.value;
    if (!id) {
        return;
    }
    apiGet("collaboratorMonitoring", { collaboratore: id }, function (data) {
        renderCollaboratorMonitoring(data);
    }, function (msg) {
        showMsg(collabMsg, msg, true);
    });
});

function renderCollaboratorMonitoring(data) {
    clear(collabTables);
    collabTables.appendChild(el("h3", { text: "Collaboratore: " + data.collaboratorName }));

    if (!data.projects || data.projects.length === 0) {
        collabTables.appendChild(el("p", { "class": "muted",
            text: "Questo collaboratore non lavora in alcun tuo progetto." }));
        return;
    }

    each(data.projects, function (proj) {
        collabTables.appendChild(el("h4", { text: "Progetto: " + proj.title }));
        var table = el("table", { "class": "grid" });

        var head = el("tr");
        head.appendChild(el("th", { text: "WP / Task" }));
        each(proj.months, function (m) {
            head.appendChild(el("th", { text: "M" + m }));
        });
        table.appendChild(head);

        each(proj.wpGroups, function (wp) {
            var wpRow = el("tr", { "class": "wp-row" });
            var wpCell = el("td", { text: wp.label });
            wpCell.setAttribute("colspan", String(proj.months.length + 1));
            wpRow.appendChild(wpCell);
            table.appendChild(wpRow);

            each(wp.tasks, function (task) {
                var tr = el("tr");
                tr.appendChild(el("td", { "class": "task-label", text: task.label }));
                each(task.worked, function (w) {
                    tr.appendChild(el("td", { text: String(w) }));
                });
                table.appendChild(tr);
            });
        });
        collabTables.appendChild(table);
    });
}

/* ----------------------- avvio ----------------------- */
showView("view-home");