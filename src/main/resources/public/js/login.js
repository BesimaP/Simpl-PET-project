// login.js — JavaScript kun til login.html (US5)

// --- Tjek at begge felter er udfyldt, før formularen sendes ---
// (om brugernavn/adgangskode er RIGTIGT kan kun backend svare på - det kommer senere)

// FIND formularen, brugernavn-feltet og adgangskode-feltet
const loginForm = document.querySelector("form");
const usernameField = document.getElementById("username");
const passwordField = document.getElementById("password");

// FIND det tomme <p id="login-error"> under felterne (fejlbeskeden skrives derind)
const loginError = document.getElementById("login-error");

// NÅR formularen sendes (tryk på "Log ind" eller Enter):
loginForm.addEventListener("submit", function (event) {

    // slet en evt. gammel fejlbesked
    loginError.textContent = "";

    // HVIS brugernavn-feltet er tomt ELLER adgangskode-feltet er tomt:  (|| = "eller")
    if (usernameField.value === "" || passwordField.value === "") {

        // vis fejl under felterne
        loginError.textContent = "Udfyld både brugernavn og adgangskode.";

        // og stop formularen i at blive sendt
        event.preventDefault();
    }
});
