const CACHE_NAME = "offline-cache-v1";
const URLS_TO_CACHE = [
    "/",
    "/menu.css",
    "/table.css",
    "/createUpdateForm",
    "/createForm.html",
    "/listForm",
    "/viewForm.html",
    "/offlineForms.html",
    "/offlinePending.js",
];

// Install service worker & cache resources
self.addEventListener("install", (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME).then((cache) => {
            return Promise.all(
                [
                    "/",  
                    "/menu.css",
                    "/table.css",
                    "/createUpdateForm",
                    "/createForm.html",
                    "/listForm",
                    "/viewForm.html",
                    "/offlineForms.html",
                    "/offlinePending.js",
                ].map(url =>
                    cache.add(url).catch(err => console.warn("Skipping failed cache:", url, err))
                )
            );
            
        })
    );
});

// Activate service worker & clean old caches
self.addEventListener("activate", (event) => {
    event.waitUntil(
        caches.keys().then((keys) => {
            return Promise.all(
                keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key))
            );
        })
    );
});

// Intercept network requests
self.addEventListener("fetch", (event) => {
    event.respondWith(
        fetch(event.request).catch(() => caches.match(event.request))
    );
});

self.addEventListener("install", (event) => {
    event.waitUntil(
        caches.open("offline-cache-v1").then((cache) => {
            console.log("Caching files...");
            return Promise.all(
                [
                    "/",  
                    "/menu.css",
                    "/table.css",
                    "/createUpdateForm",
                    "/createForm.html",
                    "/listForm",
                    "/viewForm.html",
                    "/offlineForms.html",
                    "/offlinePending.js",
                ].map(url => 
                    cache.add(url).catch(err => console.warn("Skipping failed cache:", url, err))
                )
            );
        })
    );
});
