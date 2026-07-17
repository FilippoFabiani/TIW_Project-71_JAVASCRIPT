document.getElementById('logoutBtn').addEventListener('click', function(e) {
    e.preventDefault(); // Evita che il browser segua il link vuoto "#"
    
    // 1. Pulisci i dati lato client
    sessionStorage.clear(); // Elimina username e position dal browser
    
    // 2. Reindirizza alla Servlet per pulire la sessione lato server
    window.location.href = 'Logout'; 
});