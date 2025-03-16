package parcial;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinThymeleaf;
import jakarta.persistence.PersistenceException;
import parcial.logic.Form;
import parcial.logic.User;
import parcial.services.BootStrapServices;
import parcial.services.FormServices;
import parcial.services.UserServices;

public class App {

    private static String CONNECTION_METHOD = "";

    public static void main(String[] args) {

        System.out.println("PARCIAL 2");

        if (CONNECTION_METHOD.isEmpty()) {
            BootStrapServices.getInstance().init();
        }

        try {
            UserServices.getInstance().create(new User("admin", "admin"));
            UserServices.getInstance().create(new User("mita", "123"));
            FormServices.getInstance().create(new Form("Administrator", "Other", "Other", "19.4309773", "-70.6814298", UserServices.getInstance().find("1")));

            FormServices.getInstance().create(new Form("Maria", "Sector 1", "Universitario", "19.4306666", "-70.6814444", UserServices.getInstance().find("2")));

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        var app = Javalin.create(/* HABILITAR RUTA ESTATICA */ javalinConfig -> {
            javalinConfig.staticFiles.add("/public", Location.CLASSPATH);
            javalinConfig.fileRenderer(new JavalinThymeleaf());
        }).start(1000);

        app.before("/", ctx -> {
            ctx.header("Access-Control-Allow-Origin", "*");
            ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
            ctx.header("Access-Control-Allow-Credentials", "true");

            User user = ctx.sessionAttribute("USER");
            
            if (user != null) {
                ctx.redirect("/listForm");
            } else {
                ctx.redirect("/login.html");
            }

        });

        app.error(404, ctx -> {
            try {
                String html = new String(Files.readAllBytes(Paths.get("src/main/resources/public/404.html")));
                ctx.html(html);
            } catch (Exception e) {
                ctx.result("404 - Page Not Found");
            }
        });
        
        app.get("/map", ctx -> {
            try {
                double lat = Double.parseDouble(ctx.queryParam("lat")); // Get latitude from query parameter
                double lon = Double.parseDouble(ctx.queryParam("lon")); // Get longitude from query parameter
                ctx.render("/public/map.html", Map.of("lat", lat, "lon", lon)); // Pass coordinates to the template
            } catch (Exception e) {
                ctx.result("Error: " + e.getMessage());
            }
        });
        
        app.post("/syncForms", ctx -> {
            try {
                String requestBody = ctx.body();
                System.out.println("Received JSON: " + requestBody); // ✅ Log JSON for debugging

                Gson gson = new Gson();
                JsonObject requestBodyJson = gson.fromJson(requestBody, JsonObject.class);
                JsonArray formsArray = requestBodyJson.getAsJsonArray("data");

                for (JsonElement formElement : formsArray) {
                    JsonObject formJson = formElement.getAsJsonObject();
                    System.out.println("Parsed Form: " + formJson); // ✅ Log parsed form

                    Form form = new Form();
                    form.setName(formJson.get("name").getAsString());
                    form.setSector(formJson.get("sector").getAsString());
                    form.setScholarLevel(formJson.get("scholarLevel").getAsString());
                    form.setLatitude(formJson.get("latitude").getAsString());
                    form.setLongitude(formJson.get("longitude").getAsString());

                    // Handle user association if exists
                    if (formJson.has("user")) {
                        JsonObject userJson = formJson.getAsJsonObject("user");
                        User user = UserServices.getInstance().findByUsername(userJson.get("username").getAsString());
                        form.setUser(user);
                    }

                    FormServices.getInstance().create(form);
                }

                ctx.result("{\"status\": \"success\"}");
            } catch (Exception e) {
                e.printStackTrace();
                ctx.result("{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}");
            }
        });

        

        app.post("/verifyLogin", ctx -> {
            String username = ctx.formParam("username");
            String password = ctx.formParam("password");

            try {
                User user = null;
                user = UserServices.getInstance().findByUsername(username);

                if (user != null) {
                    ctx.sessionAttribute("USER", user);
                    if (user.getPassword().equals(password)) {
                        ctx.redirect("/listForm");
                    } else {
                        ctx.result("Error: Couldn't work out user");
                    }
                } else {
                    ctx.result("Error: Incorrect Password or Username");
                    TimeUnit.SECONDS.sleep(3);
                    ctx.redirect("/");
                }

            } catch (Exception e) {

                System.out.println(e);
                e.printStackTrace();
                ctx.result("Error: " + e.getMessage());
            }

        });

        app.get("/listForm", ctx -> {
            User user = ctx.sessionAttribute("USER");

            if (user == null) {
                ctx.redirect("/login.html");
                return;
            }
            List<Form> forms = FormServices.getInstance().findAll();

            Map<String, Object> model = new HashMap<>();
            model.put("USER", user.getUsername());
            model.put("formList", forms);

            try {
                ctx.render("/public/viewForm.html", model);
            } catch (Exception e) {

                ctx.result("Error: " + e.getMessage());
            }
        });

        app.get("/listUser", ctx -> {
            User user = ctx.sessionAttribute("USER");

            if (user == null) {
                ctx.redirect("/login.html");
                return;
            }

            List<User> users = UserServices.getInstance().findAll();

            //System.out.println(users.size());

            Map<String, Object> model = new HashMap<>();
            model.put("USER", user.getUsername());
            model.put("userList", users);

            try {
                ctx.render("/public/viewUser.html", model);
            } catch (Exception e) {

                ctx.result("Error: " + e.getMessage());
            }
        });

        app.post("/createUpdateUser", ctx -> {
            User user = UserServices.getInstance().find(ctx.formParam("id"));

            Map<String, Object> model = new HashMap<>();
            model.put("user", user);

            try {
                ctx.render("/public/createUser.html", model);
            } catch (Exception e) {

                ctx.result("Error: " + e.getMessage());
            }
        });

        app.post("/createUser", ctx -> {
            String username = ctx.formParam("username");
            String password = ctx.formParam("password");

            User temp = new User(username, password);

            try {
                UserServices.getInstance().create(temp);
            } catch (PersistenceException | IllegalArgumentException e) {

                System.out.println(e.getMessage());
                ctx.result("error" + e.getMessage());
            }

            ctx.redirect("/listUser");
        });

        app.post("/createUpdateForm", ctx -> {
            Form form = FormServices.getInstance().find(ctx.formParam("id"));

            Map<String, Object> model = new HashMap<>();
            model.put("form", form);

            try {
                ctx.render("/public/createForm.html", model);
            } catch (Exception e) {

                ctx.result("Error: " + e.getMessage());
            }
        });

        app.post("/createForm", ctx -> {
            User user = ctx.sessionAttribute("USER");
        
            if (user == null) {
                ctx.redirect("/login.html");
                return;
            }
        
            // Extract form data
            String name = ctx.formParam("name");
            String sector = ctx.formParam("sector");
            String scholarLevel = ctx.formParam("scholarLevel");
            String longitude = ctx.formParam("longitude");
            String latitude = ctx.formParam("latitude");
            User currentUser = UserServices.getInstance().findByUsername(user.getUsername());
        

            if (currentUser == null) {
                ctx.result("{\"status\": \"error\", \"message\": \"User not found\"}");
                return;
            }
            // Create Person and Form objects
            Form form = new Form(name, sector, scholarLevel, longitude, latitude, user);
        
            // Save the form data to IndexedDB (via frontend)
            // Redirect to offlineForms.html for review
            ctx.redirect("/offlineForms.html");
        });


        app.post("/editForm", ctx -> {

            long id = Long.parseLong(ctx.formParam("id"));
            String name = ctx.formParam("name");
            String sector = ctx.formParam("sector");
            String scholarLevel = ctx.formParam("scholarLevel");

            Form form = FormServices.getInstance().find(id);

            form.setName(name);
            form.setSector(sector);
            form.setScholarLevel(scholarLevel);

            try {
                FormServices.getInstance().edit(form);
            } catch (Exception e) {

                System.out.println(e.getMessage());
                ctx.result("> " + e.getMessage());
            }

            ctx.redirect("/listForm");
        });

        app.post("/deleteForm", ctx -> {
            Form tmp = FormServices.getInstance().find(ctx.formParam("id"));
            boolean deleted = false;

            try {
                deleted = FormServices.getInstance().delete(tmp.getId());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(">" + e.getMessage());
                ctx.result("error" + e.getMessage());
            }

            if (deleted) {
                ctx.redirect("/listForm");
            } else {
                ctx.result("Error: No se pudo eliminar el formulario.");
            }
        });


        app.post("/editUser", ctx -> {

            long id = Long.parseLong(ctx.formParam("id"));
            String username = ctx.formParam("username");
            String password = ctx.formParam("password");

            User user = UserServices.getInstance().find(id);

            user.setUsername(username);
            user.setPassword(password);

            try {
                UserServices.getInstance().edit(user);
            } catch (Exception e) {

                System.out.println(e.getMessage());
                ctx.result("> " + e.getMessage());
            }

            ctx.redirect("/listUser");
        });

        app.post("/deleteUser", ctx -> {
            User tmp = UserServices.getInstance().find(ctx.formParam("id"));
            boolean deleted = false;

            if (!tmp.getUsername().equals("admin")) {
                try {

                    deleted = UserServices.getInstance().delete(tmp.getId());
                } catch (Exception e) {

                    e.printStackTrace();
                    System.out.println(">" + e.getMessage());
                    ctx.result("error" + e.getMessage());
                }

            }

            if (deleted) {
                ctx.redirect("/listUser");
            } else {
                ctx.result("Error: No se pudo eliminar el usuario.");
            }
        });

    }

    /**
     * Nos
     * 
     * @return
     */
    public static String getConnectionMethod() {
        return CONNECTION_METHOD;
    }
}
