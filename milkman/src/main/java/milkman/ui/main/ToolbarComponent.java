package milkman.ui.main;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.inject.Singleton;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import lombok.Value;
import lombok.val;
import milkman.domain.Environment;
import milkman.domain.Workspace;
import milkman.ui.commands.AppCommand;
import milkman.utils.Event;
import com.jfoenix.controls.JFXButton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;

import static milkman.utils.fxml.FxmlBuilder.*;
@Singleton
public class ToolbarComponent implements Initializable {

	@FXML ChoiceBox<ChoiceboxEntry> workspaceSelection;

	public final Event<AppCommand> onCommand = new Event<AppCommand>();

	@FXML ChoiceBox<ChoiceboxEntry> environmentSelection;

	@FXML JFXButton syncBtn;

	
	
	public interface ChoiceboxEntry {
		public void invoke();
		public boolean isSelectable();
	}
	public static class ChoiceBoxSeparator extends Separator implements ChoiceboxEntry {
		@Override public void invoke() {}
		@Override public boolean isSelectable() {return false;}
	} 
	@Value
	public static class ChoiceboxEntryImpl implements ChoiceboxEntry{
		String name;
		Boolean selectable;
		Runnable action;
		public String toString() {
			return name;
		}
		@Override
		public void invoke() {
			action.run();
		}
		@Override
		public boolean isSelectable() {
			return selectable;
		}
	}
	
	public void setWorkspaces(Workspace activeWs, List<String> workspaceNames) {
		initWorkspaceDropdown(activeWs, workspaceNames);
		syncBtn.setVisible(activeWs.getSyncDetails().isSyncActive());
		initEnvironmentDropdown(activeWs.getEnvironments());
		
	}

	public void initEnvironmentDropdown(List<Environment> environments) {
		environmentSelection.getItems().clear();
		
		boolean noActiveEnv = environments.stream().noneMatch(e -> e.isActive());
		
		
		val noEnvEntry = new ChoiceboxEntryImpl("No Environment", true, () -> {
			if (!noActiveEnv)
				onCommand.invoke(new AppCommand.ActivateEnvironment(Optional.empty()));	
		});
		environmentSelection.getItems().add(noEnvEntry);

		ChoiceboxEntryImpl activeEntry = null;
		for(Environment env : environments) {
			if (env.isGlobal())
				continue;
			
			ChoiceboxEntryImpl newEntry;
			if (env.isActive()) {
				activeEntry = newEntry = new ChoiceboxEntryImpl(env.getName(), true, () -> {});
			} else {
				newEntry = new ChoiceboxEntryImpl(env.getName(), true, () -> onCommand.invoke(new AppCommand.ActivateEnvironment(Optional.of(env))));
			}
			environmentSelection.getItems().add(newEntry);
		}
		environmentSelection.getItems().add(new ChoiceBoxSeparator());
		environmentSelection.getItems().add(new ChoiceboxEntryImpl("Manage Environments...", false, () ->  onCommand.invoke(new AppCommand.ManageEnvironments())));
		environmentSelection.getItems().add(new ChoiceboxEntryImpl("Edit current Env...", false, () ->  onCommand.invoke(new AppCommand.EditCurrentEnvironment())));
		environmentSelection.setValue(activeEntry != null ? activeEntry : noEnvEntry);
	}

	private void initWorkspaceDropdown(Workspace activeWs, List<String> workspaceNames) {
		workspaceSelection.getItems().clear();
		ChoiceboxEntryImpl activeEntry = null;
		for(String ws : workspaceNames) {
			ChoiceboxEntryImpl newEntry;
			if (ws.equals(activeWs.getName())) {
				activeEntry = newEntry = new ChoiceboxEntryImpl(ws, true, () -> {});
			} else {
				newEntry = new ChoiceboxEntryImpl(ws, true, () -> onCommand.invoke(new AppCommand.LoadWorkspace(ws)));
			}
			workspaceSelection.getItems().add(newEntry);
		}
		workspaceSelection.getItems().add(new ChoiceBoxSeparator());
		workspaceSelection.getItems().add(new ChoiceboxEntryImpl("Manage Workspaces...", false, () ->  onCommand.invoke(new AppCommand.ManageWorkspaces())));
		workspaceSelection.setValue(activeEntry);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		workspaceSelection.getSelectionModel().selectedItemProperty()
		.addListener((obs, o, n) -> changed(o,n, workspaceSelection));
		
		environmentSelection.getSelectionModel().selectedItemProperty()
		.addListener((obs, o, n) -> changed(o,n, environmentSelection));
	}

	public void changed(ChoiceboxEntry o, ChoiceboxEntry n, ChoiceBox<ChoiceboxEntry> choiceBox) {
		if (n != null) 
			n.invoke();
		
		if (o != null && n != null && !n.isSelectable()) {
			//select old value, if this one is unselectable
			Platform.runLater(() -> choiceBox.setValue(o));
		}
	}
	
	public void refreshEnvironments() {
		
	}

	@FXML public void onImport() {
		onCommand.invoke(new AppCommand.RequestImport());
	}

	@FXML public void onOptions() {
		onCommand.invoke(new AppCommand.ManageOptions());
	}

	@FXML public void onSync() {
		syncBtn.setDisable(true);
		syncBtn.setText("Syncing...");
		onCommand.invoke(new AppCommand.SyncWorkspace(() -> {
			syncBtn.setDisable(false);
			syncBtn.setText("Sync");
		}));
	}
	
	public static class ToolbarComponentFxml extends ToolBar {
		public ToolbarComponentFxml(ToolbarComponent controller) {
			this.setId("toolbar");
			this.getItems().add(new Label("Workspace:"));

			controller.workspaceSelection = choiceBox("workspaceSelection");
			this.getItems().add(controller.workspaceSelection);
			
			controller.syncBtn = button("syncButton", "Sync", controller::onSync);
			this.getItems().add(controller.syncBtn);
			
			this.getItems().add( button("Import", controller::onImport));
			

			this.getItems().add(new Label("Environment:"));
			
			controller.environmentSelection = choiceBox("environmentSelection");
			this.getItems().add(controller.environmentSelection);
			
			this.getItems().add( button(icon(FontAwesomeIcon.WRENCH), controller::onOptions));
			
			
			controller.initialize(null, null);
		}
	}
	
	
}
