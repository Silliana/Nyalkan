package net.nyalkan.vulkan.shader;

import net.nyalkan.Initializer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;
import org.lwjgl.util.shaderc.ShadercIncludeResolveI;
import org.lwjgl.util.shaderc.ShadercIncludeResult;
import org.lwjgl.util.shaderc.ShadercIncludeResultReleaseI;
import org.lwjgl.vulkan.VK12;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memASCII;
import static org.lwjgl.util.shaderc.Shaderc.*;

public class SPIRVUtils {
    private static final boolean DEBUG = false;
    private static final boolean OPTIMIZATIONS = true;

    private static long compiler;
    private static long options;

    //The dedicated Includer and Releaser Inner Classes used to Initialise #include Support for ShaderC
    private static final ShaderIncluder SHADER_INCLUDER = new ShaderIncluder();
    private static final ShaderReleaser SHADER_RELEASER = new ShaderReleaser();
    private static final long pUserData = 0;

    private static ObjectArrayList<String> includePaths;

    private static float time = 0.0f;

    static {
        initCompiler();
    }

    private static void initCompiler() {
        compiler = shaderc_compiler_initialize();

        if(compiler == NULL) {
            throw new RuntimeException("Failed to create shader compiler");
        }

        options = shaderc_compile_options_initialize();

        if(options == NULL) {
            throw new RuntimeException("Failed to create compiler options");
        }

        if(OPTIMIZATIONS)
            shaderc_compile_options_set_optimization_level(options, shaderc_optimization_level_performance);

        if(DEBUG)
            shaderc_compile_options_set_generate_debug_info(options);

        shaderc_compile_options_set_target_env(options, shaderc_env_version_vulkan_1_2, VK12.VK_API_VERSION_1_2);

        includePaths = new ObjectArrayList<>();
        addIncludePath("/assets/nyalkan/shaders/include/");
    }

    public static void addIncludePath(String path) {
        URL url = SPIRVUtils.class.getResource(path);

        if (url != null) {
            // union fs drops slash
            String externalForm = url.toExternalForm();
            includePaths.add(externalForm.endsWith("/") ? externalForm : externalForm + "/");
        }
    }

    public static SPIRV compileShaderAbsoluteFile(String shaderFile, ShaderKind shaderKind) {
        try {
            String source = new String(Files.readAllBytes(Paths.get(new URI(shaderFile))));
            // native include callback crashes
            source = preprocessIncludes(source, new java.util.HashSet<>());
            return compileShader(shaderFile, source, shaderKind);
        } catch (IOException | URISyntaxException e) {
            Initializer.LOGGER.error("Could not read shader {}", shaderFile, e);
        }
        return null;
    }

    // recursive visited stops cycles
    private static String preprocessIncludes(String source, java.util.Set<String> visited) {
        StringBuilder result = new StringBuilder(source.length());

        for (String line : source.split("\n", -1)) {
            String trimmed = line.trim();

            if (trimmed.startsWith("#include")) {
                int firstQuote = trimmed.indexOf('"');
                int lastQuote = trimmed.lastIndexOf('"');

                // moj import uses brackets
                if (firstQuote < 0) {
                    firstQuote = trimmed.indexOf('<');
                    lastQuote = trimmed.indexOf('>');
                }

                if (firstQuote >= 0 && lastQuote > firstQuote) {
                    String includeName = trimmed.substring(firstQuote + 1, lastQuote);

                    if (!visited.add(includeName)) {
                        throw new RuntimeException("Circular #include detected: " + includeName);
                    }

                    result.append(preprocessIncludes(resolveInclude(includeName), visited)).append('\n');
                    continue;
                }
            }

            result.append(line).append('\n');
        }

        return result.toString();
    }

    private static String resolveInclude(String requested) {
        for (String includePath : includePaths) {
            try {
                Path path = Paths.get(new URI(includePath + requested));

                if (Files.exists(path)) {
                    return new String(Files.readAllBytes(path));
                }
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        throw new RuntimeException("Unable to find " + requested + " in include paths");
    }

    public static SPIRV compileShader(String filename, String source, ShaderKind shaderKind) {
        long startTime = System.nanoTime();

        // mod shaders skip expansion
        source = preprocessIncludes(source, new java.util.HashSet<>());

        long result = shaderc_compile_into_spv(compiler, source, shaderKind.kind, filename, "main", options);

        if(result == NULL) {
            throw new RuntimeException("Failed to compile shader " + filename + " into SPIR-V");
        }

        if(shaderc_result_get_compilation_status(result) != shaderc_compilation_status_success) {
            throw new RuntimeException("Failed to compile shader " + filename + " into SPIR-V:\n" + shaderc_result_get_error_message(result));
        }

        time += (System.nanoTime() - startTime) / 1000000.0f;

        return new SPIRV(result, shaderc_result_get_bytes(result));
    }

    private static SPIRV readFromStream(InputStream inputStream) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes);
            buffer.position(0);

            return new SPIRV(MemoryUtil.memAddress(buffer), buffer);
        } catch (Exception e) {
            throw new RuntimeException("unable to read inputStream", e);
        }
    }

    public enum ShaderKind {
        VERTEX_SHADER(shaderc_glsl_vertex_shader),
        GEOMETRY_SHADER(shaderc_glsl_geometry_shader),
        FRAGMENT_SHADER(shaderc_glsl_fragment_shader),
        COMPUTE_SHADER(shaderc_glsl_compute_shader);

        private final int kind;

        ShaderKind(int kind) {
            this.kind = kind;
        }
    }

    private static class ShaderIncluder implements ShadercIncludeResolveI {

        private static final int MAX_PATH_LENGTH = 4096; //Maximum Linux/Unix Path Length

        @Override
        public long invoke(long user_data, long requested_source, int type, long requesting_source, long include_depth) {
            var requesting = memASCII(requesting_source);
            var requested = memASCII(requested_source);

            // freed by releaser
            try {
                for (String includePath : includePaths) {
                    Path path = Paths.get(new URI(String.format("%s%s", includePath, requested)));

                    if (Files.exists(path)) {
                        byte[] bytes = Files.readAllBytes(path);

                        ByteBuffer nameBuffer = MemoryUtil.memASCII(requested);
                        ByteBuffer contentBuffer = MemoryUtil.memAlloc(bytes.length).put(bytes).flip();

                        return ShadercIncludeResult.calloc()
                                .source_name(nameBuffer)
                                .content(contentBuffer)
                                .user_data(user_data).address();
                    }
                }
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }

            throw new RuntimeException(String.format("%s: Unable to find %s in include paths", requesting, requested));
        }
    }

    private static class ShaderReleaser implements ShadercIncludeResultReleaseI {

        @Override
        public void invoke(long user_data, long include_result) {
            ShadercIncludeResult result = ShadercIncludeResult.create(include_result);
            MemoryUtil.memFree(result.source_name());
            MemoryUtil.memFree(result.content());
            MemoryUtil.nmemFree(include_result);
        }
    }

    public static final class SPIRV implements NativeResource {

        private final long handle;
        private ByteBuffer bytecode;

        public SPIRV(long handle, ByteBuffer bytecode) {
            this.handle = handle;
            this.bytecode = bytecode;
        }

        public ByteBuffer bytecode() {
            return bytecode;
        }

        @Override
        public void free() {
//            shaderc_result_release(handle);
            bytecode = null; // Help the GC
        }
    }

}