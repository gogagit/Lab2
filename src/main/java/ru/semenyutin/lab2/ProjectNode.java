package ru.semenyutin.lab2;
import java.time.LocalDate;
import java.util.*;

public sealed abstract class ProjectNode permits ProjectNode.Group,ProjectNode.Task {
    public record Totals(int tasks,int done,int minutes,int doneMinutes) {
        Totals plus(Totals t){return new Totals(tasks+t.tasks,done+t.done,Math.addExact(minutes,t.minutes),Math.addExact(doneMinutes,t.doneMinutes));}
    }
    public static final class TreeException extends IllegalArgumentException {
        TreeException(String message){super(message);}
    }
    private String name;
    private Group parent;
    protected ProjectNode(String name){rename(name);}
    public final String name(){return name;}
    public final Group parent(){return parent;}
    public final void rename(String value){
        if(value==null||value.isBlank()||value.strip().length()>120)throw new TreeException("Название должно содержать от 1 до 120 символов.");
        name=value.strip();
    }
    public abstract Totals totals();
    public abstract void complete(boolean done);
    public void add(ProjectNode child){throw new TreeException("У задачи не может быть дочерних элементов.");}
    public static final class Group extends ProjectNode {
        private final List<ProjectNode> children=new ArrayList<>();
        public Group(String name){super(name);}
        public List<ProjectNode> children(){return List.copyOf(children);}
        @Override public void add(ProjectNode child){
            if(child==null||child.parent!=null)throw new TreeException("Элемент уже в дереве или отсутствует.");
            int depth=0;
            for(ProjectNode p=this;p!=null;p=p.parent()){if(p==child)throw new TreeException("Циклы запрещены.");depth++;}
            if(depth+height(child)>32)throw new TreeException("Максимальная глубина дерева — 32.");
            children.add(child);child.parent=this;
        }
        private static int height(ProjectNode p){return p instanceof Group g?1+g.children.stream().mapToInt(Group::height).max().orElse(0):1;}
        public void remove(ProjectNode child){
            if(!children.remove(child))throw new TreeException("Элемент не принадлежит этой группе.");child.parent=null;
        }
        @Override public Totals totals(){Totals t=new Totals(0,0,0,0);for(ProjectNode c:children)t=t.plus(c.totals());return t;}
        @Override public void complete(boolean done){for(ProjectNode c:children)c.complete(done);}
    }
    public static final class Task extends ProjectNode {
        private int minutes;
        private LocalDate due;
        private boolean done;
        public Task(String name,int minutes,LocalDate due){super(name);configure(name,minutes,due);}
        public void configure(String name,int minutes,LocalDate due){
            if(minutes<1||minutes>60000||due==null)throw new TreeException("Укажите срок и оценку от 1 до 60000 минут.");
            rename(name);this.minutes=minutes;this.due=due;
        }
        public int minutes(){return minutes;}
        public LocalDate due(){return due;}
        public boolean done(){return done;}
        @Override public Totals totals(){return new Totals(1,done?1:0,minutes,done?minutes:0);}
        @Override public void complete(boolean done){this.done=done;}
    }
    @Override public String toString(){return name;}
}
