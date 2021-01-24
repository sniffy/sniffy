package io.sniffy.nio;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import io.sniffy.util.JVMUtil;
import io.sniffy.util.ReflectionUtil;
import sun.nio.ch.SelChImpl;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.HashSet;

/**
 * @since 3.1.7
 */
public class SniffySelectorProviderModule {

    public static void initialize() {

        if (JVMUtil.getVersion() <= 7) return;

        if (JVMUtil.getVersion() == 8 && Boolean.getBoolean("io.sniffy.forceJava7Compatibility")) return;

        if (JVMUtil.getVersion() >= 16) {
            // TODO: only open required package

            try {
                Class<?> moduleClass = Class.forName("java.lang.Module");
                Method export = Module.class.getDeclaredMethod("implAddOpens",String.class);
                ReflectionUtil.setAccessible(export);
                HashSet<Module> modules = new HashSet();
                Class selChImplClass = Class.forName("sun.nio.ch.SelChImpl");
                Module base = selChImplClass.getModule();
                if (base.getLayer() != null)
                    modules.addAll(base.getLayer().modules());
                modules.addAll(ModuleLayer.boot().modules());
                for (ClassLoader cl = selChImplClass.getClassLoader(); cl != null; cl = cl.getParent()) {
                    modules.add(cl.getUnnamedModule());
                }
                for (Module module : modules) {
                    for (String name : module.getPackages()) {
                        try {
                            export.invoke(module,name);
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        try {
            SniffySelectorProvider.install();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
