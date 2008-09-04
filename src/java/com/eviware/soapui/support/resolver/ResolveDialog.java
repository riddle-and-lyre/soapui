/*
 *  soapUI, copyright (C) 2004-2008 eviware.com 
 *
 *  soapUI is free software; you can redistribute it and/or modify it under the 
 *  terms of version 2.1 of the GNU Lesser General Public License as published by 
 *  the Free Software Foundation.
 *
 *  soapUI is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU Lesser General Public License for more details at gnu.org.
 */

package com.eviware.soapui.support.resolver;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.TreeMap;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.JXTable;

import com.eviware.soapui.impl.wsdl.AbstractWsdlModelItem;
import com.eviware.soapui.impl.wsdl.actions.project.SimpleDialog;
import com.eviware.soapui.support.UISupport;
import com.eviware.soapui.support.resolver.ResolveContext.PathToResolve;
import com.eviware.soapui.support.resolver.ResolveContext.Resolver;

/**
 * Utility for resolving items
 *
 * @author Ole.Matzura
 */

public class ResolveDialog
{

	private JDialog dialog;
   private ResolveContextTableModel resolveContextTableModel;
   private boolean showOkMessage;
   private String title;
   private String description;
   private String helpUrl;
   private Resolver[] defaultResolvers = null;

   public ResolveDialog( String title, String description, String helpUrl, Resolver...resolvers )
   {
      this.title = title;

      this.description = description;
      this.helpUrl = helpUrl;
      
      this.defaultResolvers = resolvers;
   }

   private void buildDialog()
   {
      dialog = new SimpleDialog( title, description, helpUrl )
      {
         @Override
         protected Component buildContent()
         {
            JPanel panel = new JPanel( new BorderLayout() );
            JXTable table = new JXTable( resolveContextTableModel );
            table.setHorizontalScrollEnabled( true );
            table.setDefaultRenderer( JComboBox.class, new ResolverRenderer() );
            table.setDefaultEditor( JComboBox.class, new ResolverEditor() );
            table.getColumn( 2 ).setCellRenderer( new PathCellRenderer() );
            table.getColumn( 3 ).setWidth( 100 );

            panel.add( new JScrollPane( table ), BorderLayout.CENTER );
            panel.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );

            return panel;
         }

         @Override
         protected boolean handleOk()
         {
         	//needs refactor
         	for( PathToResolve path : resolveContextTableModel.getContext().getPathsToResolve() ) {
         		int index = resolveContextTableModel.getContext().getPathsToResolve().indexOf(path);
         		Object key = resolveContextTableModel.jbcMap.get(index).getSelectedItem();
         		if( resolveContextTableModel.resolverz.get(key) instanceof Resolver ) {
         			path.setResolver(resolveContextTableModel.resolverz.get(key));
         		}
         	}
            int cnt = resolveContextTableModel.getContext().apply();
            if( isShowOkMessage() )
            {
               UISupport.showInfoMessage( "Resolved " + cnt + " items" );
            }
            return true;
         }

			@Override
			protected boolean handleCancel()
			{
				resolveContextTableModel.getContext().cancel();
				
				return super.handleCancel();
			}
         
         
      };

      dialog.setSize( 550, 300 );
   }

   public boolean isShowOkMessage()
   {
      return showOkMessage;
   }

   public void setShowOkMessage( boolean showOkMessage )
   {
      this.showOkMessage = showOkMessage;
   }

   public ResolveContext resolve( AbstractWsdlModelItem modelItem )
   {
      ResolveContext context = new ResolveContext( modelItem );
      modelItem.resolve( context );
      if( context.isEmpty() )
      {
         if( isShowOkMessage() )
         {
            UISupport.showInfoMessage( "No resolve problems found", title );
         }
      }
      else
      {
         resolveContextTableModel = new ResolveContextTableModel( context );
         if( dialog == null )
            buildDialog();

         UISupport.centerDialog( dialog );
         dialog.setVisible( true );
      }

      return context;
   }

   @SuppressWarnings("serial")
	private class ResolveContextTableModel extends AbstractTableModel
   {
      private ResolveContext<?> context;
      private ArrayList<JComboBox> jbcMap = new ArrayList<JComboBox>();
      TreeMap<String, Resolver> resolverz = new TreeMap<String, Resolver>();

      public ResolveContextTableModel( ResolveContext<?> context2 )
      {
         context = context2;
         for( PathToResolve path: context.getPathsToResolve() ) {
         	ArrayList<Object> resolversAndDefaultAction = new ArrayList<Object>();
         	if ( path.getDefaultAction() != null ) {
         		resolversAndDefaultAction.add(path.getDefaultAction().getDescription());
         	}
         	for( Resolver resolver: defaultResolvers ) {
         		resolversAndDefaultAction.add(resolver.getDecription());
         		resolverz.put((String) resolver.getDecription(), resolver);
         	}
         	if( path.getResolver() != null ) {
         		resolversAndDefaultAction.add(path.getResolver().getDecription());
         	}
         	JComboBox jbc = new JComboBox(resolversAndDefaultAction.toArray());
         	jbcMap.add(jbc);
         }
         
      }
      
      public JComboBox getResolversAndActions(int row) {
//      	PathToResolve path = context.getPathsToResolve().get(row);
//      	int index = context.getPathsToResolve().indexOf(path);
      	return jbcMap.get(row);
      }

      public int getColumnCount()
      {
         return 4;
      }

      public void setContext( ResolveContext context )
      {
         this.context = context;
         fireTableDataChanged();
      }

      @Override
      public String getColumnName( int column )
      {
         switch( column )
         {
            case 0:
               return "Item";
            case 1:
               return "Description";
            case 2:
               return "Value";
            case 3:
               return "Action";
         }

         return super.getColumnName( column );
      }

      @Override
      public Class<?> getColumnClass( int arg0 )
      {
         if( arg0 == 3 )
            return JComboBox.class;
         else
            return String.class;
      }

      public int getRowCount()
      {
         return context.getPathsToResolve().size();
      }

      @Override
      public boolean isCellEditable( int rowIndex, int columnIndex )
      {
//         return columnIndex == 3 && context.getPathsToResolve().get( rowIndex ).getResolver() != null;
      	return columnIndex == 3;
      }

      public Object getValueAt( int arg0, int arg1 )
      {
         PathToResolve ptr = context.getPathsToResolve().get( arg0 );
         switch( arg1 )
         {
            case 0:
               return ptr.getOwner().getName();
            case 1:
               return ptr.getDescription();
            case 2:
               return ptr.getPath();
            case 3:
               return ptr.getResolver();
            
         }

         return null;
      }

      public ResolveContext<?> getContext()
      {
         return context;
      }

		public void setResolver(int pathIndex, Object resolveOrDefaultAction)
		{
			PathToResolve path = context.getPathsToResolve().get(pathIndex);
			if ( resolveOrDefaultAction instanceof Resolver ) {
				path.setResolver(resolveOrDefaultAction);
			}
			
		}
   }

   private class ResolverRenderer implements TableCellRenderer
   {
//      private JButton button = new JButton( "Resolve..." );
//      private JLabel label = new JLabel( "No Resolution" );

      public Component getTableCellRendererComponent( JTable table,
                                                      Object value, boolean isSelected, boolean hasFocus, int row,
                                                      int column )
      {
//         return value == null ? label : button;
      	return ((ResolveContextTableModel)table.getModel()).getResolversAndActions(row);
      }
   }

   private class ResolverEditor extends AbstractCellEditor implements TableCellEditor//, ActionListener
   {
//      private Resolver resolver;
//      private JButton button;
      private JComboBox jbc = new JComboBox();
      
      private ResolverEditor()
      {
//         button = new JButton( "Resolve..." );
//         button.addActionListener( this );
//         jbc.addActionListener(this);
      }

      public Component getTableCellEditorComponent( JTable table, Object value, boolean isSelected, int row, int column )
      {
//         this.resolver = (Resolver) arg1;
//         return button;
      	jbc = ((ResolveContextTableModel)table.getModel()).getResolversAndActions(row);
//      	((ResolveContextTableModel)table.getModel()).setResolver(row, jbc.getSelectedItem());
         return jbc;
      }

      public Object getCellEditorValue()
      {
      	return jbc.getSelectedItem();
      }

//      public void actionPerformed( ActionEvent arg0 )
//      {
//         if( resolver.resolve() )
//            resolveContextTableModel.fireTableDataChanged();
//      }
   }

   private class PathCellRenderer extends DefaultTableCellRenderer
   {
      private Color greenColor = Color.GREEN.darker().darker();
      private Color redColor = Color.RED.darker().darker();

      @Override
      public Component getTableCellRendererComponent( JTable arg0, Object arg1, boolean arg2, boolean arg3, int arg4,
                                                      int arg5 )
      {
         Component comp = super.getTableCellRendererComponent( arg0, arg1, arg2, arg3, arg4, arg5 );

         PathToResolve ptr = resolveContextTableModel.getContext().getPathsToResolve().get( arg4 );
         boolean resolved = ptr.getResolver() != null && ptr.getResolver().isResolved();

         if( resolved )
         {
            setForeground( greenColor );
            setText( ptr.getResolver().getResolvedPath() );
         }
         else
         {
            setForeground( redColor );
         }

         return comp;
      }
   }

   public Resolver[] getResolvers()
   {
   	return defaultResolvers;
   }
   
   public void setResolvers(Resolver[] resolvers)
   {
   	this.defaultResolvers = resolvers;
   }
   
}