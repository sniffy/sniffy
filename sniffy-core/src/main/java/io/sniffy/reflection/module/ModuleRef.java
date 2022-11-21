package io.sniffy.reflection.module;

import static io.sniffy.reflection.Unsafe.$;

public class ModuleRef {

    private final /* Module */ Object module;
    private final Throwable throwable;

    public ModuleRef(/* Module */ Object module, Throwable throwable) {

        assert "java.lang.Module".equals(module.getClass().getName());

        this.module = module;
        this.throwable = throwable;
    }

    public void addOpens(String packageName) throws Exception {
        $("java.lang.Module").method("implAddOpens", String.class).invoke(module, packageName);
    }

}
