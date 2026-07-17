"use strict";

/*
 * =============================================================================
 *  NUCLEO CONDIVISO DELL'APPLICAZIONE  (versione JavaScript / RIA)
 * =============================================================================
 *
 * Scelte architetturali (dalle slide del corso):
 *
 *  - UNICA VARIABILE GLOBALE: tutto vive dentro un solo oggetto contenitore,
 *    "TIW", per non inquinare lo scope globale ed evitare interferenze tra
 *    script diversi.
 *
 *  - CLASSI DI OGGETTI, NON RACCOLTE DI FUNZIONI: le utilita' del DOM sono
 *    raggruppate in un oggetto-modulo (TIW.dom); i componenti con stato e
 *    comportamento sono realizzati come "classi", cioe' funzioni costruttore
 *    invocate con "new", con i metodi definiti sul prototype e riferiti a "this".
 *
 *  - COMUNICAZIONE ASINCRONA: le chiamate al server usano XMLHttpRequest e
 *    scambiano dati in formato JSON (JSON.parse / stringify nativi).
 *
 *  - EVENTI: nessun attributo inline (onclick=...) e nessuna proprieta' diretta
 *    (element.onclick). Si usa sempre addEventListener e, come listener, un
 *    OGGETTO dotato del metodo handleEvent, cosi' "this" punta al componente.
 *
 *  Questo file e' incluso da TUTTE le pagine e definisce le parti riusabili;
 *  ogni pagina aggiunge poi il proprio script specifico.
 * =============================================================================
 */

/* Unica variabile globale: contenitore dell'intera applicazione. */
var TIW = TIW || {};

/* ========================================================================== *
 *  TIW.dom - modulo di utilita' per la manipolazione del DOM                   *
 *  (object literal: le funzioni sono metodi di un unico oggetto)               *
 * ========================================================================== */
TIW.dom = {

    /* Itera su una lista "array-like" (es. NodeList) invocando fn(elemento, i). */
    each: function (list, fn) {
        for (var i = 0; i < list.length; i++) {
            fn(list[i], i);
        }
    },

    /* Svuota un nodo rimuovendone tutti i figli. */
    clear: function (node) {
        while (node.firstChild) {
            node.removeChild(node.firstChild);
        }
    },

    /*
     * Crea un elemento. props supporta le chiavi speciali "text" (textContent)
     * e "class" (className); ogni altra chiave diventa un attributo. children e'
     * un array di nodi da appendere.
     */
    el: function (tag, props, children) {
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
    },

    /* Riempie una <select> con un placeholder e la lista di {value, label}. */
    fillSelect: function (select, options, placeholder) {
        this.clear(select);
        select.appendChild(this.el("option", { value: "", text: placeholder || "Seleziona..." }));
        for (var i = 0; i < options.length; i++) {
            select.appendChild(this.el("option", {
                value: String(options[i].value),
                text: options[i].label
            }));
        }
    },

    /* Riporta una <select> allo stato iniziale ("--"), opzionalmente disabilitata. */
    resetSelect: function (select, disable) {
        this.clear(select);
        select.appendChild(this.el("option", { value: "", text: "--" }));
        if (disable) {
            select.disabled = true;
        }
    },

    /* Scrive un messaggio (ok/errore) in un nodo, con la relativa classe CSS. */
    showMsg: function (node, text, isError) {
        node.textContent = text || "";
        node.className = "msg " + (isError ? "msg-err" : "msg-ok");
    },

    /* Codifica un array di coppie [chiave, valore] in application/x-www-form-urlencoded. */
    encodeParams: function (pairs) {
        var parts = [];
        for (var i = 0; i < pairs.length; i++) {
            parts.push(encodeURIComponent(pairs[i][0]) + "=" + encodeURIComponent(pairs[i][1]));
        }
        return parts.join("&");
    }
};

/* ========================================================================== *
 *  TIW.Http - "classe" che incapsula una chiamata asincrona (XHR + JSON)       *
 * ========================================================================== *
 *
 *  Uso:
 *     new TIW.Http().request({
 *         method: "GET" | "POST",
 *         url: "...",
 *         headers: { ... },            // opzionale
 *         body: FormData | String,     // opzionale (null per GET)
 *         onSuccess: function (data, req) { ... },   // 2xx, data = JSON o null
 *         onError:   function (message, req) { ... }, // != 2xx (401 escluso se gestito)
 *         onUnauthorized: function (req) { ... }      // opzionale: status 401
 *     });
 */
TIW.Http = function () {
    /* Nessuno stato: ogni request crea un proprio XMLHttpRequest (via closure). */
};

TIW.Http.prototype.request = function (options) {
    var req = new XMLHttpRequest();
    req.open(options.method, options.url, true);

    if (options.headers) {
        var keys = Object.keys(options.headers);
        for (var i = 0; i < keys.length; i++) {
            req.setRequestHeader(keys[i], options.headers[keys[i]]);
        }
    }

    /* La closure cattura req e options: il componente resta "this" nei callback. */
    req.onreadystatechange = function () {
        if (req.readyState !== XMLHttpRequest.DONE) {
            return;
        }

        /* Prova a interpretare il corpo come JSON; se non lo e', resta null. */
        var data = null;
        try {
            data = req.responseText ? JSON.parse(req.responseText) : null;
        } catch (e) {
            data = null;
        }

        if (req.status === 401 && typeof options.onUnauthorized === "function") {
            options.onUnauthorized(req);
            return;
        }

        if (req.status >= 200 && req.status < 300) {
            if (typeof options.onSuccess === "function") {
                options.onSuccess(data, req);
            }
        } else if (typeof options.onError === "function") {
            var message = (data && data.error)
                ? data.error
                : (req.responseText || ("Errore " + req.status));
            options.onError(message, req);
        }
    };

    req.send(options.body !== undefined && options.body !== null ? options.body : null);
};

/* ========================================================================== *
 *  TIW.LogoutController - oggetto-listener (handleEvent) per il bottone Logout  *
 * ========================================================================== *
 *
 *  Intercetta il click sul link/bottone di logout: pulisce la memoria del
 *  browser (sessionStorage) e reindirizza alla servlet Logout, che distrugge
 *  la sessione lato server.
 */
TIW.LogoutController = function (buttonId) {
    this.button = document.getElementById(buttonId || "logoutBtn");
    if (this.button) {
        this.button.addEventListener("click", this);
    }
};

TIW.LogoutController.prototype.handleEvent = function (event) {
    event.preventDefault();          // impedisce di seguire il link "#"
    sessionStorage.clear();          // rimuove username/position dal browser
    window.location.href = "Logout"; // la servlet ripulisce la sessione Java
};