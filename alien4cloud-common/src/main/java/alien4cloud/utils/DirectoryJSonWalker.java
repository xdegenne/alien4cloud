package alien4cloud.utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class DirectoryJSonWalker {
    private DirectoryJSonWalker() {
    }

    /**
     * Walk a directory to build a json that describe the structure.
     * 
     * @param directory The directory to walk.
     * @param target The path in which to save the json file.
     * @throws IOException In case of an IO issue while walking the directory.
     */
    public static void directoryJson(Path directory, Path target) throws IOException {
        final TreeNode root = new TreeNode();
        root.setLeaf(false);
        root.setFullPath("");
        root.setChildren(new ArrayList<TreeNode>());
        // create the directory layout file
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            TreeNode current = root;

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                TreeNode treeNode = new TreeNode();
                treeNode.setLeaf(false);
                treeNode.setName(dir.getFileName().toString());
                treeNode.setFullPath(current.getFullPath() + "/" + dir.getFileName().toString());
                treeNode.setChildren(new ArrayList<TreeNode>());
                treeNode.setParent(current);
                current.getChildren().add(treeNode);
                current = treeNode;
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                current = current.getParent();
                return super.postVisitDirectory(dir, exc);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                TreeNode treeNode = new TreeNode();
                treeNode.setLeaf(true);
                treeNode.setName(file.getFileName().toString());
                treeNode.setFullPath(current.getFullPath() + "/" + file.getFileName().toString());
                treeNode.setChildren(null);
                current.getChildren().add(treeNode);
                return super.visitFile(file, attrs);
            }
        });
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(target.toFile(), root);
    }
}