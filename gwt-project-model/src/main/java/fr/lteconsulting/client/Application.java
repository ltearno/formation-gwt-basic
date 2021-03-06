package fr.lteconsulting.client;

import java.util.List;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

import fr.lteconsulting.client.map.Map;
import fr.lteconsulting.shared.Personne;
import fr.lteconsulting.shared.PersonnesService;
import fr.lteconsulting.shared.PersonnesServiceAsync;

public class Application implements EntryPoint
{
	private IFormulairePersonne formulaire = new FormulairePersonneUiBinder();
	private Button okButton = new Button( "Valider" );
	private Button nouveauButton = new Button( "Nouveau" );
	private Button effacerButton = new Button( "Effacer" );

	private CellList<Personne> cellList;
	private Personne editedPersonne;

	private PersonnesServiceAsync personnesService = GWT.create( PersonnesService.class );

	private ListDataProvider<Personne> dataProvider = new ListDataProvider<>();

	@Override
	public void onModuleLoad()
	{
		GWT.setUncaughtExceptionHandler( e -> Window.alert( "Exception " + e ) );

		Scheduler.get().scheduleFixedDelay( () -> {
			if( Map.googleMapsInitialized() )
			{
				return false;
			}
			return true;
		}, 250 );

		initUi();

		personnesService.getPersonnes( new AsyncCallback<List<Personne>>()
		{
			@Override
			public void onSuccess( List<Personne> result )
			{
				dataProvider.getList().clear();
				dataProvider.getList().addAll( result );
			}

			@Override
			public void onFailure( Throwable caught )
			{
				Window.alert( "Une erreur s'est produite : " + caught );
			}
		} );
	}

	private void continueInit()
	{
		initHandlers();

		dataProvider.addDataDisplay( cellList );
	}

	private void initUi()
	{
		cellList = new CellList<Personne>( new AbstractCell<Personne>()
		{
			@Override
			public void render( Context context, Personne value, SafeHtmlBuilder sb )
			{
				sb.append( PersonneTemplate.INSTANCE.display(
						value.getPrenom(),
						value.getNom() ) );
			}
		} );
		cellList.setKeyboardSelectionPolicy( KeyboardSelectionPolicy.ENABLED );
		cellList.setPageSize( 500 );

		MenuBar fileMenu = new MenuBar( true );

		fileMenu.addItem( "Générer", () -> Window.alert( "Not yet implemented" ) );

		MenuBar menu = new MenuBar();
		menu.addItem( "File", fileMenu );

		VerticalPanel vp = new VerticalPanel();
		vp.add( formulaire );
		vp.add( okButton );
		vp.add( nouveauButton );
		vp.add( effacerButton );

		DockLayoutPanel layout = new DockLayoutPanel( Unit.EM );
		layout.addNorth( menu, 2 );
		layout.addWest( new ScrollPanel( cellList ), 14 );
		layout.add( new ScrollPanel( vp ) );

		RootLayoutPanel.get().add( layout );

		continueInit();
	}

	private void initHandlers()
	{
		SingleSelectionModel<Personne> selectionModel = new SingleSelectionModel<>();
		cellList.setSelectionModel( selectionModel );
		selectionModel.addSelectionChangeHandler( event -> {
			editedPersonne = selectionModel.getSelectedObject();
			formulaire.updateFormFromPersonne( editedPersonne );
		} );

		okButton.addClickHandler( event -> {
			formulaire.updatePersonneFromForm( editedPersonne );

			int personneIndex = dataProvider.getList().indexOf( editedPersonne );
			dataProvider.getList().set( personneIndex, editedPersonne );
		} );

		nouveauButton.addClickHandler( event -> {
			Personne personne = new Personne();
			formulaire.updatePersonneFromForm( personne );

			personnesService.createPersonne( personne, new AsyncCallback<Personne>()
			{
				@Override
				public void onSuccess( Personne result )
				{
					dataProvider.getList().add( result );
				}

				@Override
				public void onFailure( Throwable caught )
				{
					Window.alert( "aie ! " + caught );
				}
			} );
		} );

		effacerButton.addClickHandler( new ClickHandler()
		{
			@Override
			public void onClick( ClickEvent event )
			{
				if( editedPersonne == null )
					return;

				personnesService.deletePersonne( editedPersonne.getId(), new AsyncCallback<Boolean>()
				{
					@Override
					public void onSuccess( Boolean result )
					{
						if( result )
						{
							dataProvider.getList().remove( editedPersonne );
							selectionModel.setSelected( editedPersonne, false );
							editedPersonne = null;
						}
					}

					@Override
					public void onFailure( Throwable caught )
					{
						Window.alert( "aie ! " + caught );
					}
				} );
			}
		} );
	}
}