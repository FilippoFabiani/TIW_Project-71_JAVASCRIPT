### Viste e componenti
**Login**
- form login
	- field text
	- field password
- wizard
	- scelta profilo se è un tecnico, compare il form select (sul responsabile di default)
	
**Home Amministratore**
- form progetto
	- field text titolo
	- field number durata
	- field text responsabile
	- field submit SALVA
- form creazione WP
	- field select progetto
	- field text numero d'ordine
	- field select mese inizio
	- field select mese fine
	- field submit SALVA
- field creazione task
	- field select progetto
	- field select WP
	- field text numero d'ordine
	- field text descrizione
	- field select mese inizio
	- field select mese fine
	- field submit SALVA
- field submit di logout
- field submit verifica progetti
- messaggio welcome
- wizard
	- i bottoni si evidenziano al completamento di tutti i campi richiesti per la creazione specifica
	- dopo la creazione del primo progetto compare un bottone per la verifica progetti; oppure compare nel singolo form di creazione (WP e TASK) per vedere il progetto che si sta modificando

**Verifica progetti**
- display del titolo di progetto
- field select project
- gerarchia di composizione del progetto
- form submit logout
- form submit ritorno alla home

**Home responsabile**
- form assegnazione collaboratori
	- field select progetto
	- field select wp
	- field select task
	- field select mese
	- field text collaboratore
	- field number ore
	- field submit per salvare
	- field submit per assegnare collaboratori
- messaggio di errore nell'assegnamento (è un wizard?)
-  form submit logout
- form submit pagina monitoraggio progetti
- form submit pagina monitoraggio collaboratori
- messaggio welcome
- wizard
	- dopo aver riempito il form viene abilitato il bottone SALVA
	- dopo aver riempito tutte le task del progetto compare il bottone ASSEGNA


**Monitoraggio progetti**
- tabella stato completamento dei task
- field select progetto da monitorare
- titolo progetto
- field submit logout
- field submit pagina home
- field submit CONCLUDI
- wizard
	- compare il bottone CONCLUDI se il totale delle ore svolte è corretto
	- compare la tabella del progetto dopo che è stato selezionato

**Monitoraggio collaboratori**
- field select collaboratore dei progetti di cui è responsabile
- tabelle stato dei task
- titolo progetto
- form submit logout
- form submit home

**Home collaboratore**
- lista cliccabile progetti cui è  assegnato
- lista cliccabile wp
- lista cliccabile task
- form task
	- field select mese
	- field number ore lavorate
	- field submit SALVA
- form submit logout
- messaggio welcome
- wizard
	- compare la selezione wp dopo la selezione progetto
	- compare la selezione task in sostituizione della wp
	- compare il rimpimento task con il bottone salva

### Eventi e azioni
 | client ||| server|||
|-|-|-|-|-|-
|*evento*|*azione*|*controllore*|*evento*|*azione*|*controllore*
|*login*
|index→login form.submit|check||post(un,pw)|check cred|checkLogin
|wizard→submit|reindirizza tecnico
|*home amministratore*
|amministratore→load|form creazione vuoti|Page Orchestrator|get(-)|estrazione progetti, wp|getAdminData
|amministratore→submit progetto|check dati,svuota form|makeCall|POST(titolo, durata,resp)|check,INSERT|createProject
|amministratore→submit wp|check dati,svuota form|makeCall|POST(progetto,n ordine, mese inizio,mese fine)|check,INSERT|createWp
|aministratore→submit task|check dati,svuota form|makeCall|POST(progetto, wp,n ordine, mese inizio,mese fine, descrizione)|check,INSERT|createTask
|amministratore→form wp→selezione progetto|abilita form wp|-|-|-
|amministratore→form task→selezione progetto,wp|abilita form task|-|-|-
|logout|||GET|terminazione sessione|Logout
|*verifica progetti*
|verifica→load|mostra progetto di default|Page Orchestrator|GET(admin, -)|estrazione progetti|getProjectList
|verifica → selezione progetto|mostra albero progetto|????|GET(project, *admin)|check, estrazione progetto|GetProject
|home|torna a home amministratore|Page Orchestrator→refresh|-|-|-
|logout|||GET|terminazione sessione|Logout
|*verifica progetti*
|verifica→load|mostra progetto di default|Page Orchestrator|GET(admin, -)|estrazione progetti|GetAdminProject
|verifica → selezione progetto|mostra albero progetto|????|GET(project, *admin)|check, estrazione progetto|GetAdminProject
|home|torna a home amministratore|Page Orchestrator→refresh|-|-|-
|logout|||GET|terminazione sessione|Logout
|*home collaboratore*
|collaboratore → load|mostra task di default||GET(*collab)|check collab, restituisce lista dei progetti|GetCollabProjects
|collaboratore → form progetto → select|refresh, rende visibile in tendina le i wp||-|-|-
|collaboratore → form progetto → select|refresh, rende visibile in tendina i wp||-|-|-
|collaboratore → form progetto → form wp → select|refresh, rende visibile in tendina le task||-|-|-
|collaboratore → form progetto → form wp → form task → select|rende visibile il form per assegnare le ore||-|-|-
|bottone SALVA → click|manda i dati||POST(\*collab, \*titolo,\*wp,\*task, mese, ore)|check, mette le ore nella base|SetHours
|logout|||GET|terminazione sessione|Logout
|*home responsabile*
|responsabile→load|mostra gli elenchi a tendina vuoti|Page Orchestrator|get(-)|estrazione progetti del responsabile con relativi wp e task| getMenagerData
|responsabile→form assegnazione collaboratori→selezione progetto|abilita selezione wp|
|responsabile→form assegnazione collaboratori→selezione wp|abilita selezione task|
|responsabile→form assegnazione collaboratori→selezione task|abilita selezione mese|
|responsabile→form assegnazione collaboratori→selezione mese|abilita selezione collaboratore|
|responsabile→form assegnazione collaboratori→submit salva|check dati,salva in locale|-|-|-|-
|responsabile→form assegnazione collaboratori→submit salva|check dati,svuota form|makeCall|POST(progetto, wp, task, mese, collaboratore, ore)|check,INSERT|AssigneProject (Servlet)
|responsabile→form pagina monitoraggio progetti|cambia pagina con monitoraggio progetti|-|-|-
|responsabile→form pagina monitoraggio collaboratori|cambia pagina con monitoraggio collaboratori|-|-|-
|*monitoraggio progetti*
|monitoraggio responsabile→load|carica la tabella con gli stati dei task, mostra elenco a tendina e campo titolo vuoto|Page Orchestrator|get(-)|estrazione progetti del responsabile| GetManagerData (Servlet)
|monitoraggio responsabile→field select progetto da monitorare→select|viene caricato il totale delle ore, se sufficiente appare il bottone concludi|Page Orchestrator→refresh|-|-|-
|monitoraggio responsabile→field submit CONLCUDI→submit|viene caricato il totale delle ore, se sufficiente appare il bottone concludi|Page Orchestrator→refresh, make call|POST(progetto, stato)|check,INSERT|ChangeProjectState (Servlet)
|*monitoraggio collaboratori*
|monitoraggio collaboratori→load|viene caricato il menù a tendina vuoto, e le tabelle vuote|Page Orchestrator|get(-)|estrazione dei collaboratori dei relativi progetti del responsabile| GetProjectCollaboratorsData (Servlet)
|monitoraggio collaboratori→field select collaboratore →select|viene mostrata la tabella dei collaboratori con relativi task |Page Orchestrator→refresh|-|-|-

### Componenti JS
- ProjectList
- WpList
- TaskList
- CollaboratorList

### Server
**Servlet**
- CheckLogin
- CreateProject
- CreateWp
- CreateTask
- GetAdminData
- GetProject
- GetAdminProject
- GetCollabProjects
- SetHours
- Logout

*Amministratore*
- CreateProject
- CreateWp
- CreateTask
- GetManagerData
- GetProject
- GetWP
- GetTask
- Logout

*Responsabile, monitoraggio progetti, monitoraggio collaboratori*

- GetManagerData
- AssigneProject
- ChangeProjectState
- GetProjectCollaboratorsData

**Beans**

- Project
	- title
	- term
	- status
	- collaborators list\<Collaborator>
	- manager
- WorkPackage
	- id_order
	- title
	- startMonth
	- endMonth
	- tasks list\<Task>
- Task
	- idOrder
	- title
	- description
	- startMonth
	- endMonth
	- collaborators list\<Collaborator>
- Technical
	- username
	- name
	- surname
	- picture
	- projects list\<Project>
- Amministrator
	- username
	- projects

**DAO**
Mi raccomando, i DAO fanno riferimento al database, quindi devono essere relativi alla tabella del Data Base
	
- ProjectDAO
	- changeProjectStatus
	- findProjectCollaborators
- UserDAO
	- public checkCredentials(us, pw)
	- public checkPosition(us)
- CollaboratorDAO
	- getCollabProjectList
	- setHours
- ManagerDAO
	- findManagerProjects

- AdministratorDAO
	- public saveProject (titolo, durata, responsabile, lista wp, lista task)
	- private createProject(titolo, durata, responsabile)
	- private createWp(titolo, mese inizio, mese fine)
	- private createTask(titolo, numero d'ordine, mese inizio, mese fine, descrizione)
	- public getAdminProject( titolo progetto )

### Completamento delle specifiche


### Default
**Login**
nel form di inserimento campi "username" e "password"
