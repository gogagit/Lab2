package ru.semenyutin.lab2;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.Properties;

public final class Main extends Application {
    private final TreeView<ProjectNode> tree=new TreeView<>();
    private final TextField name=new TextField(),minutes=new TextField("60");
    private final DatePicker due=new DatePicker(LocalDate.now().plusDays(3));
    private final Label totals=new Label(),status=new Label("Выберите раздел или задачу.");
    private final ProgressBar progress=new ProgressBar();
    private ProjectNode.Group root;
    public static void main(String[] args){launch(args);}
    @Override public void start(Stage stage)throws Exception{
        Properties settings=new Properties();try(var in=getClass().getResourceAsStream("/application.properties")){settings.load(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8));}
        root=new ProjectNode.Group(settings.getProperty("project.name"));
        var program=new ProjectNode.Group("Программа выставки");program.add(new ProjectNode.Task("Подготовить схему зала",90,LocalDate.now().plusDays(3)));root.add(program);
        var t=new ProjectNode.Task("Согласовать тему",45,LocalDate.now());t.complete(true);root.add(t);
        tree.setCellFactory(v->new TreeCell<>(){protected void updateItem(ProjectNode n,boolean empty){super.updateItem(n,empty);setText(empty||n==null?null:n instanceof ProjectNode.Group?"▸ "+n.name():(((ProjectNode.Task)n).done()?"✓ ":"○ ")+n.name());}});
        tree.getSelectionModel().selectedItemProperty().addListener((o,a,item)->{
            if(item==null)return;ProjectNode n=item.getValue();name.setText(n.name());
            if(n instanceof ProjectNode.Task task){minutes.setText(""+task.minutes());due.setValue(task.due());}
        });
        var save=Fx.button("Сохранить",()->action(()->{var p=Fx.file(stage,true,"Сохранить проект","*.properties");if(p!=null){ProjectFile.save(p,root);status.setText("Проект сохранён.");}}));
        var open=Fx.button("Открыть",()->action(()->{var p=Fx.file(stage,false,"Открыть проект","*.properties");if(p!=null){root=ProjectFile.load(p);rebuild(root);status.setText("Проект открыт.");}}));
        var form=new VBox(10,new Label("Название"),name,new Label("Минуты и срок задачи"),new HBox(8,minutes,due),
          new HBox(8,Fx.button("+ Раздел",()->add(false)),Fx.button("+ Задача",()->add(true))),
          new HBox(8,Fx.button("Применить",()->action(()->{var n=selected();if(n instanceof ProjectNode.Task task)task.configure(name.getText(),estimate(),due.getValue());else n.rename(name.getText());tree.refresh();update();})),
          Fx.button("Удалить",()->action(()->{var n=selected();var p=n.parent();if(p==null)throw new IllegalArgumentException("Корень удалить нельзя.");p.remove(n);rebuild(p);}))),
          new HBox(8,Fx.button("Завершить ветвь",()->complete(true)),Fx.button("Вернуть в работу",()->complete(false))),status);
        form.getStyleClass().add("panel");
        SplitPane split=new SplitPane(tree,form);split.setDividerPositions(.48);
        var pane=new VBox(12,Fx.title("План выставки"),new HBox(10,open,save),totals,progress,split);
        pane.setPadding(new Insets(18));VBox.setVgrow(split,Priority.ALWAYS);progress.setMaxWidth(Double.MAX_VALUE);
        rebuild(root);Fx.show(stage,"Лабораторная 2 · План выставки",pane,960,600);
    }
    private interface Job{void run()throws Exception;}
    private void action(Job j){try{j.run();}catch(Exception e){Fx.error(e);}}
    private ProjectNode selected(){var i=tree.getSelectionModel().getSelectedItem();return i==null?root:i.getValue();}
    private int estimate(){try{return Integer.parseInt(minutes.getText().strip());}catch(NumberFormatException e){throw new IllegalArgumentException("Введите целое число минут.");}}
    private void add(boolean task){action(()->{var p=selected();ProjectNode n=task?new ProjectNode.Task(name.getText(),estimate(),due.getValue()):new ProjectNode.Group(name.getText());p.add(n);rebuild(n);});}
    private void complete(boolean value){selected().complete(value);tree.refresh();update();}
    private TreeItem<ProjectNode> item(ProjectNode n,ProjectNode focus){
        var t=new TreeItem<>(n);t.setExpanded(true);
        if(n instanceof ProjectNode.Group g)for(var c:g.children())t.getChildren().add(item(c,focus));
        return t;
    }
    private TreeItem<ProjectNode> locate(TreeItem<ProjectNode> t,ProjectNode n){if(t.getValue()==n)return t;for(var c:t.getChildren()){var a=locate(c,n);if(a!=null)return a;}return null;}
    private void rebuild(ProjectNode n){tree.setRoot(item(root,n));tree.getSelectionModel().select(locate(tree.getRoot(),n));update();}
    private void update(){var t=root.totals();totals.setText("Выполнено: "+t.done()+" / "+t.tasks()+"    Минуты: "+t.doneMinutes()+" / "+t.minutes());progress.setProgress(t.tasks()==0?0:(double)t.done()/t.tasks());}
}
