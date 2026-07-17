(function () {
  var loginForm = document.getElementById("loginForm");
  var roleChoice = document.getElementById("roleChoice");

  document.getElementById("loginbutton").addEventListener('click', function (e) {
    var form = e.target.closest("form");
    if (!form.checkValidity()) { form.reportValidity(); return; }

    makeCall("POST", 'CheckLogin', form, function (req) {
      if (req.readyState !== XMLHttpRequest.DONE) return;
      if (req.status === 200) {
        var data = JSON.parse(req.responseText);
        sessionStorage.setItem('username', data.username);
        route(data.position);
      } else {
        document.getElementById("errormessage").textContent = req.responseText;
      }
    });
  });

  function route(position) {
    switch (position) {
      case 'ADMIN':        window.location.href = 'homeAdmin';           break; // nuovo DOM
      case 'MANAGER':      window.location.href = 'homeManager';         break; // nuovo DOM
      case 'COLLABORATOR': window.location.href = 'homeCollaboratorJS';  break; // nuovo DOM
      case 'TECHNICIAN':   showRoleChoice();                             break; // resta, muta il DOM
      default:
        document.getElementById("errormessage").textContent = "Ruolo non riconosciuto";
    }
  }

  // UNICO caso senza redirect: sia responsabile sia collaboratore -> scelta.
  function showRoleChoice() {
    loginForm.style.display = 'none';
    roleChoice.innerHTML =
        '<h2>Come vuoi accedere?</h2>' +
        '<button id="goManager">Home Responsabile</button> ' +
        '<button id="goCollaborator">Home Collaboratore</button>';
    roleChoice.style.display = 'block';

    document.getElementById("goManager")
        .addEventListener('click', function () { window.location.href = 'homeManager'; });
    document.getElementById("goCollaborator")
        .addEventListener('click', function () { window.location.href = 'homeCollaboratorJS'; });
  }
})();