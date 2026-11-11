package ru.semenyutin.lab2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
class CompositeTest {
    @TempDir Path dir;
    @Test void recursivelyTotalsAndCompletes(){
        var root=new ProjectNode.Group("Выставка");var section=new ProjectNode.Group("Зал");root.add(section);
        section.add(new ProjectNode.Task("Схема",45,LocalDate.now()));root.add(new ProjectNode.Task("Список",25,LocalDate.now()));
        section.complete(true);assertEquals(new ProjectNode.Totals(2,1,70,45),root.totals());
        root.complete(true);assertEquals(new ProjectNode.Totals(2,2,70,70),root.totals());
    }
    @Test void rejectsCyclesAndSharedChildren(){
        var root=new ProjectNode.Group("Корень");var section=new ProjectNode.Group("Секция");root.add(section);
        assertThrows(ProjectNode.TreeException.class,()->section.add(root));
        assertThrows(ProjectNode.TreeException.class,()->root.add(section));
    }
    @Test void roundTrip()throws Exception{
        var root=new ProjectNode.Group("Выставка");var task=new ProjectNode.Task("Декор",30,LocalDate.of(2026,12,1));root.add(task);root.complete(true);
        var f=dir.resolve("project.properties");ProjectFile.save(f,root);var loaded=ProjectFile.load(f);
        assertEquals(root.totals(),loaded.totals());assertEquals("Декор",loaded.children().get(0).name());
    }
}
