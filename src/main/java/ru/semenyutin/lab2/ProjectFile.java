package ru.semenyutin.lab2;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Properties;

final class ProjectFile {
    private ProjectFile(){}
    static void save(Path path,ProjectNode.Group root)throws IOException {
        Properties p=new Properties();p.setProperty("format","1");write(p,"root",root);
        try(Writer out=Files.newBufferedWriter(path,StandardCharsets.UTF_8)){p.store(out,"Project tree");}
    }
    private static void write(Properties p,String key,ProjectNode n){
        p.setProperty(key+".name",n.name());
        if(n instanceof ProjectNode.Group g){
            p.setProperty(key+".type","group");p.setProperty(key+".count",""+g.children().size());
            for(int i=0;i<g.children().size();i++)write(p,key+"."+i,g.children().get(i));
        }else{
            var t=(ProjectNode.Task)n;p.setProperty(key+".type","task");p.setProperty(key+".minutes",""+t.minutes());
            p.setProperty(key+".due",t.due().toString());p.setProperty(key+".done",""+t.done());
        }
    }
    static ProjectNode.Group load(Path path)throws IOException {
        if(Files.size(path)>2_000_000)throw new IOException("Файл проекта слишком большой.");
        Properties p=new Properties();
        try(Reader in=Files.newBufferedReader(path,StandardCharsets.UTF_8)){p.load(in);}
        catch(IllegalArgumentException e){throw new IOException("Некорректный файл проекта.",e);}
        try{
            if(!"1".equals(p.getProperty("format")))throw new IllegalArgumentException();
            if(read(p,"root",0,new int[]{0}) instanceof ProjectNode.Group g)return g;
            throw new IllegalArgumentException();
        }catch(RuntimeException e){throw new IOException("Некорректная структура файла проекта.",e);}
    }
    private static ProjectNode read(Properties p,String k,int depth,int[] count){
        if(depth>=32||++count[0]>10000)throw new IllegalArgumentException();
        String name=p.getProperty(k+".name");
        return switch(p.getProperty(k+".type","")){
            case "group" -> {
                var g=new ProjectNode.Group(name);int n=Integer.parseInt(p.getProperty(k+".count"));
                if(n<0||n>10000)throw new IllegalArgumentException();
                for(int i=0;i<n;i++)g.add(read(p,k+"."+i,depth+1,count));
                yield g;
            }
            case "task" -> {
                var t=new ProjectNode.Task(name,Integer.parseInt(p.getProperty(k+".minutes")),LocalDate.parse(p.getProperty(k+".due")));
                String done=p.getProperty(k+".done","");if(!done.equals("true")&&!done.equals("false"))throw new IllegalArgumentException();
                t.complete(Boolean.parseBoolean(done));yield t;
            }
            default -> throw new IllegalArgumentException();
        };
    }
}
