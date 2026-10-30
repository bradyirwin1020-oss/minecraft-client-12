package com.aegis.client.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class ModuleManager {
    private final List<ClientModule> modules = new ArrayList<>();

    public ClientModule register(String id, String name, String description, ModuleCategory category, boolean enabled) {
        ClientModule module = new ClientModule(id, name, description, category, enabled);
        modules.add(module);
        return module;
    }

    public List<ClientModule> all() { return Collections.unmodifiableList(modules); }

    public List<ClientModule> inCategory(ModuleCategory category) {
        return modules.stream().filter(module -> module.category() == category).toList();
    }

    public Optional<ClientModule> find(String id) {
        return modules.stream().filter(module -> module.id().equals(id)).findFirst();
    }

    public boolean enabled(String id) {
        return find(id).map(ClientModule::enabled).orElse(false);
    }

    public void toggle(String id) { find(id).ifPresent(ClientModule::toggle); }
    public void setEnabled(String id, boolean enabled) { find(id).ifPresent(module -> module.setEnabled(enabled)); }
}
