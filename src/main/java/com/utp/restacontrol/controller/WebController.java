package com.utp.restacontrol.controller;

import com.utp.restacontrol.service.AtencionService;
import com.utp.restacontrol.service.CategoriaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.utp.restacontrol.service.ClienteService;
import com.utp.restacontrol.service.PlatoService;
import com.utp.restacontrol.service.CocinaService;

@Controller
public class WebController {

    private final CategoriaService categoriaService;
    private final ClienteService clienteService;
    private final PlatoService platoService;
    private final AtencionService atencionService;
    private final CocinaService cocinaService;

    public WebController(CategoriaService categoriaService, ClienteService clienteService, PlatoService platoService, AtencionService atencionService, CocinaService cocinaService) {
        this.platoService = platoService;
        this.categoriaService = categoriaService;
        this.clienteService = clienteService;
        this.atencionService = atencionService;
        this.cocinaService = cocinaService;
    }

    @GetMapping({"/", "/login"})
    public String index() {
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/categorias")
    public String categorias(Model model) {
        model.addAttribute("categorias", categoriaService.listarCategorias());
        return "categorias";
    }

    @GetMapping("/clientes")
    public String clientes(Model model) {
        model.addAttribute("clientes", clienteService.listarClientes());
        return "clientes";
    }

    @GetMapping("/platos")
    public String platos(Model model) {
        model.addAttribute("platos", platoService.listarPlatos());
        return "platos";
    }

    @GetMapping("/atenciones")
    public String atenciones(Model model) {
        model.addAttribute("atenciones", atencionService.listarAtenciones());
        return "atenciones";
    }

    @GetMapping("/cocina")
    public String cocina(Model model) {
        var items = cocinaService.listarItemsCocina();

        model.addAttribute("itemsCocina", items);
        model.addAttribute("totalItems", items.size());
        model.addAttribute("totalPendientes", items.stream().filter(i -> i.getEstado().equals("Pendiente")).count());
        model.addAttribute("totalCancelados", items.stream().filter(i -> i.getEstado().equals("Cancelado")).count());
        model.addAttribute("totalListos", items.stream().filter(i -> i.getEstado().equals("Listo para entrega")).count());
        model.addAttribute("totalEntregados", items.stream().filter(i -> i.getEstado().equals("Entregado")).count());

        return "cocina";
    }
}