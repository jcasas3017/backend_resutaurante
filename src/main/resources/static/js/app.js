const DB = (() => {
    const STORAGE_KEY = 'restacontrol-v2';

    const seeds = {
        
        
        mesas: [
            { id: 1, codigo: 'M-01', capacidad: 4, ubicacion: 'Ventana',       activa: true  },
            { id: 2, codigo: 'M-02', capacidad: 2, ubicacion: 'Salon central', activa: true  },
            { id: 3, codigo: 'T-01', capacidad: 6, ubicacion: 'Terraza',       activa: true  },
            { id: 4, codigo: 'M-03', capacidad: 8, ubicacion: 'VIP',           activa: false }
        ],
        reservas: [
            { id: 1, tipo: 'Salon',   id_cliente: 1, nombre_contacto: 'Lucia Fernandez', id_mesa: 1, fecha_hora: '2026-04-09T20:00', cantidad_personas: 4, estado: 'Confirmada', confirmada: true  },
            { id: 2, tipo: 'Terraza', id_cliente: 2, nombre_contacto: 'Carlos Ramos',    id_mesa: 3, fecha_hora: '2026-04-10T13:00', cantidad_personas: 3, estado: 'Pendiente',  confirmada: false }
        ],
        
        productos: [
            { id: 1, nombre: 'Inca Kola 500ml',  descripcion: 'Gaseosa nacional 500ml',      precio: 5.00,  stock: 50, unidad: 'unidad', activo: true },
            { id: 2, nombre: 'Coca Cola 500ml',   descripcion: 'Gaseosa importada 500ml',     precio: 5.50,  stock: 40, unidad: 'unidad', activo: true },
            { id: 3, nombre: 'Cerveza Pilsen',     descripcion: 'Cerveza lata 355ml',          precio: 9.00,  stock: 30, unidad: 'unidad', activo: true },
            { id: 4, nombre: 'Agua San Luis',      descripcion: 'Agua mineral 625ml',          precio: 3.50,  stock: 60, unidad: 'unidad', activo: true },
            { id: 5, nombre: 'Jugo de naranja',    descripcion: 'Jugo natural 300ml',          precio: 7.00,  stock: 20, unidad: 'unidad', activo: true }
        ],
        comprobantes: []
    };

    function _load() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY);
            if (!raw) { _save(seeds); return JSON.parse(JSON.stringify(seeds)); }
            return JSON.parse(raw);
        } catch { _save(seeds); return JSON.parse(JSON.stringify(seeds)); }
    }

    function _save(data) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
    }

    function getAll(entity) {
        return _load()[entity] || [];
    }

    function getById(entity, id) {
        return getAll(entity).find(r => r.id === Number(id));
    }

    function insert(entity, record) {
        const data = _load();
        const list = data[entity] || [];
        const nextId = list.reduce((m, r) => Math.max(m, r.id || 0), 0) + 1;
        const newRecord = { id: nextId, ...record };
        list.push(newRecord);
        data[entity] = list;
        _save(data);
        return newRecord;
    }

    function update(entity, id, record) {
        const data = _load();
        data[entity] = (data[entity] || []).map(r => r.id === Number(id) ? { ...r, ...record, id: Number(id) } : r);
        _save(data);
    }

    function remove(entity, id) {
        const data = _load();
        data[entity] = (data[entity] || []).filter(r => r.id !== Number(id));
        _save(data);
    }

    function reset() { _save(JSON.parse(JSON.stringify(seeds))); }

    return { getAll, getById, insert, update, remove, reset };
})();

