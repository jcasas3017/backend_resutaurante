package com.utp.restacontrol.dto.auth;

public class UsuarioResponse {
    private String username;
    private String name;
    private String role;

    public UsuarioResponse(String username, String name, String role) {
        this.username = username;
        this.name = name;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getName() { return name; }
    public String getRole() { return role; }
}
/*Javier Casas Montenegro 
U17107243
*/

