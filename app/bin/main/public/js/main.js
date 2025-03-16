// Sync data when the app comes back online
window.addEventListener("online", function () {
    console.log("App is online. Syncing data...");
    syncFormsWithServer();
});

window.addEventListener("offline", function () {
    console.log("App is offline. Data will be synced later.");
});