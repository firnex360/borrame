if ("serviceWorker" in navigator) {
    navigator.serviceWorker.register("/sw.js")
        .then(() => console.log("Service Worker registered!"))
        .catch((err) => console.error("Service Worker registration failed:", err));
}


let db;

const request = indexedDB.open("formDB", 1);

request.onupgradeneeded = function (event) {
    db = event.target.result;
    console.log("Database upgrade needed");
    //CHECK
    if (!db.objectStoreNames.contains("syncQueue")) {
        db.createObjectStore("syncQueue", { keyPath: "id", autoIncrement: true });
        console.log("Object store 'syncQueue' created");
    }
    
    if (!db.objectStoreNames.contains("forms")) {
        db.createObjectStore("forms", { keyPath: "id", autoIncrement: true });
        console.log("Object store 'forms' created");
    }
};

request.onsuccess = function (event) {
    db = event.target.result;
    console.log("Database opened successfully");
    loadPendingRecordsDashboard();
};

request.onerror = function (event) {
    console.error("Error opening database:", event.target.error);
};

document.addEventListener("DOMContentLoaded", function () {
    if (document.getElementById("offlineList")) {
        loadPendingRecordsDashboard();
    } else {
        console.warn("Skipping loadPendingRecordsDashboard: Not on offlineForms.html");
    }
});

function createForm(formData) {
    const transaction = db.transaction(["forms"], "readwrite");
    const store = transaction.objectStore("forms");
    const addRequest = store.add(formData);

    addRequest.onsuccess = function () {
        console.log("Form added to IndexedDB", formData);
        window.location.href = "/offlineForms.html";
    };

    addRequest.onerror = function (event) {
        console.error("Error adding form to IndexedDB:", event.target.error);
    };
}

function loadPendingRecordsDashboard() {
    const tbody = document.getElementById('offlineList');
    if (!tbody) {
        console.warn("Skipping loadPendingRecordsDashboard: #offlineList not found");
        return;
    }

    const transaction = db.transaction(["forms"], "readonly");
    const store = transaction.objectStore("forms");
    const request = store.getAll();

    request.onsuccess = function (event) {
        const forms = event.target.result;
        tbody.innerHTML = '';

        forms.forEach(form => {
            tbody.innerHTML += `
                <tr>
                    <td>${form.name}</td>
                    <td>${form.sector}</td>
                    <td>${form.scholarLevel}</td>
                    <td>${form.latitude}</td>
                    <td>${form.longitude}</td>
                    <td>${form.user ? form.user.username : 'admin'}</td>
                    <td>
                        <button onclick="sendPending(${form.id})" class="btn btn-success btn-sm">
                            <i class="fas fa-cloud-upload-alt"></i> Enviar
                        </button>
                        <button onclick="deletePending(${form.id})" class="btn btn-danger btn-sm">
                            <i class="fas fa-trash"></i> Eliminar
                        </button>
                    </td>
                </tr>`;
        });
    };

    request.onerror = function (event) {
        console.error("Error loading records from IndexedDB:", event.target.error);
    };
}

function deletePending(id) {
    const transaction = db.transaction(["forms"], "readwrite");
    const store = transaction.objectStore("forms");
    const request = store.delete(id);

    request.onsuccess = function () {
        console.log("Form deleted from IndexedDB");
        loadPendingRecordsDashboard();
    };

    request.onerror = function (event) {
        console.error("Error deleting form record:", event.target.error);
    };
}

function sanitizeFormData(formData) {
    let cleanedData = {};
    for (let key in formData) {
        if (formData[key] !== undefined && formData[key] !== null) {
            cleanedData[key] = String(formData[key]);  // Convert everything to string
        }
    }
    delete cleanedData.id; // 🚨 Remove id
    console.log("Sanitized JSON:", JSON.stringify({ data: [cleanedData] }, null, 2));
    return cleanedData;
}




function sendPending() {
    const transaction = db.transaction(["forms"], "readonly");
    const store = transaction.objectStore("forms");
    const request = store.getAll();

    request.onsuccess = function (event) {
        const records = event.target.result;
        if (records.length === 0) {
            console.log("No pending records found.");
            return;
        }

        let sanitizedData = { data: records.map(record => ({
            ...sanitizeFormData(record),
            originalId: record.id // Preserve the ID
        }))};

        // Try to send the data
        fetch("/syncForms", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(sanitizedData)
        })
        .then(response => response.json())
        .then(data => {
            console.log("Server response:", data);
            if (data.status === "success") {
                // Delete sent records
                const deleteTransaction = db.transaction(["forms"], "readwrite");
                const deleteStore = deleteTransaction.objectStore("forms");
                records.forEach(record => {
                    deleteStore.delete(record.id);
                    console.log("Deleted record with id:", record.id);
                });
            }
        })
        .catch(() => {
            console.warn("No internet, saving for later.");
            saveToSyncQueue(sanitizedData);
        });
    };
}

function saveToSyncQueue(formData) {
    const transaction = db.transaction(["syncQueue"], "readwrite");
    const store = transaction.objectStore("syncQueue");
    store.add(formData);
}


window.addEventListener("online", retrySyncQueue);

function retrySyncQueue() {
    const transaction = db.transaction(["syncQueue"], "readwrite");
    const store = transaction.objectStore("syncQueue");
    const request = store.getAll();

    request.onsuccess = function () {
        const pendingRequests = request.result;
        if (pendingRequests.length === 0) return;

        pendingRequests.forEach(formData => {
            fetch("/syncForms", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(formData)
            })
            .then(response => response.json())
            .then(() => {
                store.delete(formData.id);
                console.log("Resent and deleted record:", formData.id);
            })
            .catch(error => console.error("Error retrying:", error));
        });
    };
}
