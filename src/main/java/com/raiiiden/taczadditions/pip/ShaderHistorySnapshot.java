package com.raiiiden.taczadditions.pip;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// GPU-side backup of the persistent textures and SSBOs a shader pack owns. Restoring it before the
// normal pass keeps a REAL PIP pass's alternate FOV out of TAA, motion blur and shader history.
public final class ShaderHistorySnapshot implements AutoCloseable {
    public record TextureRef(int id, int target) {}
    public record BufferRef(int id, long size) {}

    private record TextureCopy(int original, int backup, int target,
                               int levels, int width, int height, int depth) {}
    private record BufferCopy(int original, int backup, long size) {}

    private final List<TextureCopy> textures = new ArrayList<>();
    private final List<BufferCopy> buffers = new ArrayList<>();
    private boolean restored;

    private ShaderHistorySnapshot() {}

    public static ShaderHistorySnapshot capture(Collection<TextureRef> textureRefs,
                                                Collection<BufferRef> bufferRefs) {
        RenderSystem.assertOnRenderThread();
        ShaderHistorySnapshot snapshot = new ShaderHistorySnapshot();
        try {
            Set<Long> seenTextures = new HashSet<>();
            for (TextureRef ref : textureRefs) {
                if (ref == null || ref.id() <= 0) continue;
                long key = ((long) ref.target() << 32) | (ref.id() & 0xFFFFFFFFL);
                if (seenTextures.add(key)) snapshot.captureTexture(ref);
            }

            Set<Integer> seenBuffers = new HashSet<>();
            for (BufferRef ref : bufferRefs) {
                if (ref == null || ref.id() <= 0 || ref.size() <= 0) continue;
                if (seenBuffers.add(ref.id())) snapshot.captureBuffer(ref);
            }
            return snapshot;
        } catch (Throwable throwable) {
            snapshot.close();
            throw throwable;
        } finally {
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            RenderSystem.bindTexture(0);
            GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, 0);
            GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, 0);
        }
    }

    private void captureTexture(TextureRef ref) {
        GL11.glBindTexture(ref.target(), ref.id());
        int width = GL11.glGetTexLevelParameteri(ref.target(), 0, GL11.GL_TEXTURE_WIDTH);
        if (width <= 0) return;
        int height = ref.target() == GL11.GL_TEXTURE_1D ? 1
                : GL11.glGetTexLevelParameteri(ref.target(), 0, GL11.GL_TEXTURE_HEIGHT);
        int depth = ref.target() == GL12.GL_TEXTURE_3D
                ? GL11.glGetTexLevelParameteri(ref.target(), 0, GL12.GL_TEXTURE_DEPTH) : 1;
        int internalFormat = GL11.glGetTexLevelParameteri(ref.target(), 0, GL11.GL_TEXTURE_INTERNAL_FORMAT);

        int levels = 1;
        if (ref.target() != GL31.GL_TEXTURE_RECTANGLE) {
            while (levels < 16
                    && GL11.glGetTexLevelParameteri(ref.target(), levels, GL11.GL_TEXTURE_WIDTH) > 0) {
                levels++;
            }
        }

        int backup = GL11.glGenTextures();
        GL11.glBindTexture(ref.target(), backup);
        if (ref.target() == GL11.GL_TEXTURE_1D) {
            GL42.glTexStorage1D(ref.target(), levels, internalFormat, width);
        } else if (ref.target() == GL12.GL_TEXTURE_3D) {
            GL42.glTexStorage3D(ref.target(), levels, internalFormat, width, height, depth);
        } else {
            GL42.glTexStorage2D(ref.target(), levels, internalFormat, width, height);
        }

        for (int level = 0; level < levels; level++) {
            int w = Math.max(1, width >> level);
            int h = ref.target() == GL11.GL_TEXTURE_1D ? 1 : Math.max(1, height >> level);
            int d = ref.target() == GL12.GL_TEXTURE_3D ? Math.max(1, depth >> level) : 1;
            GL43.glCopyImageSubData(ref.id(), ref.target(), level, 0, 0, 0,
                    backup, ref.target(), level, 0, 0, 0, w, h, d);
        }
        textures.add(new TextureCopy(ref.id(), backup, ref.target(), levels, width, height, depth));
    }

    private void captureBuffer(BufferRef ref) {
        int backup = GL15.glGenBuffers();
        GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, backup);
        GL15.glBufferData(GL31.GL_COPY_WRITE_BUFFER, ref.size(), GL15.GL_STREAM_COPY);
        GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, ref.id());
        GL31.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER,
                0, 0, ref.size());
        buffers.add(new BufferCopy(ref.id(), backup, ref.size()));
    }

    public void restore() {
        if (restored) return;
        restored = true;
        RenderSystem.assertOnRenderThread();
        try {
            for (TextureCopy copy : textures) {
                for (int level = 0; level < copy.levels(); level++) {
                    int w = Math.max(1, copy.width() >> level);
                    int h = copy.target() == GL11.GL_TEXTURE_1D ? 1 : Math.max(1, copy.height() >> level);
                    int d = copy.target() == GL12.GL_TEXTURE_3D ? Math.max(1, copy.depth() >> level) : 1;
                    GL43.glCopyImageSubData(copy.backup(), copy.target(), level, 0, 0, 0,
                            copy.original(), copy.target(), level, 0, 0, 0, w, h, d);
                }
            }
            for (BufferCopy copy : buffers) {
                GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, copy.backup());
                GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, copy.original());
                GL31.glCopyBufferSubData(GL31.GL_COPY_READ_BUFFER, GL31.GL_COPY_WRITE_BUFFER,
                        0, 0, copy.size());
            }
        } finally {
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            RenderSystem.bindTexture(0);
            GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, 0);
            GL15.glBindBuffer(GL31.GL_COPY_WRITE_BUFFER, 0);
            deleteBackups();
        }
    }

    private void deleteBackups() {
        for (TextureCopy copy : textures) GL11.glDeleteTextures(copy.backup());
        for (BufferCopy copy : buffers) GL15.glDeleteBuffers(copy.backup());
        textures.clear();
        buffers.clear();
    }

    @Override
    public void close() {
        if (!restored) {
            restored = true;
            deleteBackups();
        }
    }
}
