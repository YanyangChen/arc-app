package arc.app.Controller;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;










//import cal.aes.Model.AESmEventPlan;
import acf.acf.Abstract.ACFaAppController;
import acf.acf.Database.ACFdSQLAssDelete;
import acf.acf.Database.ACFdSQLAssInsert;
import acf.acf.Database.ACFdSQLAssSelect;
import acf.acf.Database.ACFdSQLAssUpdate;
import acf.acf.Database.ACFdSQLRule;
import acf.acf.Database.ACFdSQLRule.RuleCase;
import acf.acf.Database.ACFdSQLRule.RuleCondition;
import acf.acf.General.annotation.ACFgAuditKey;
import acf.acf.General.annotation.ACFgFunction;
import acf.acf.General.annotation.ACFgTransaction;
import acf.acf.General.core.ACFgRawModel;
import acf.acf.General.core.ACFgRequestParameters;
import acf.acf.General.core.ACFgResponseParameters;
import acf.acf.General.core.ACFgSearch;
import acf.acf.Interface.ACFiCallback;
import acf.acf.Interface.ACFiSQLAssWriteInterface;
import acf.acf.Model.ACFmGridResult;
import acf.acf.Model.ACFmUser;
import acf.acf.Static.ACFtUtility;
import arc.apf.Dao.ARCoItemInventory;
import arc.apf.Dao.ARCoItemMaster;
import arc.apf.Dao.ARCoPODetails;
import arc.apf.Dao.ARCoPOHeader;
import arc.apf.Model.ARCmIndirectBudget;
import arc.apf.Model.ARCmItemInventory;
import arc.apf.Model.ARCmItemMaster;
import arc.apf.Model.ARCmPOHeader;
import arc.apf.Service.ARCsLocation;
import arc.apf.Service.ARCsModel;
import arc.apf.Service.ARCsPoHeader;
//import arc.apw.Controller.APPc004.Search;
import arc.app.Static.APPtMapping;

@Controller
@Scope("session")
@ACFgFunction(id="APPF004")
@RequestMapping(value=APPtMapping.APPF004)
public class APPc004 extends ACFaAppController {
    
    @Autowired ARCsModel moduleService;
    //@Autowired ACFoFunction functionDao;
    @Autowired ARCoPOHeader POHeaderDao; //modify according to the table
    @Autowired ARCoPODetails PODetailsDao;
    @Autowired ARCoItemInventory ItemInventoryDao;
    @Autowired ARCoItemMaster ItemMasterDao;
    @Autowired ARCsPoHeader PoHeaderService;
    @Autowired ARCsLocation LocationService;
    //@Autowired APFsFuncGp funcGpService; //click the object and click import
    @ACFgAuditKey String purchase_order_no;
    @ACFgAuditKey String item_no;
    @ACFgAuditKey BigDecimal unit_cost;
    
  //  Search search = new Search();

    private class Search extends ACFgSearch {
        public Search() {
            super();
            setCustomSQL("select * from (select p.* from arc_po_header p where p.section_id = '04')");
            setKey("purchase_order_no");
            addRule(new ACFdSQLRule("supplier_code", RuleCondition.EQ, null, RuleCase.Insensitive));
            addRule(new ACFdSQLRule("purchase_order_no", RuleCondition.EQ, null, RuleCase.Insensitive));
          //addRule(new ACFdSQLRule("other_material", RuleCondition._LIKE_, null, RuleCase.Insensitive));
        }// ACF will forward the content to client and post to the grid which ID equals to “grid_browse”.
        @Override
        public Search setValues(ACFgRequestParameters param) throws Exception { //use the search class to setup an object
            super.setValues(param);// param is a object, "Search" 's mother class passed
                if(!param.isEmptyOrNull("start_date")) {
                wheres.and("purchase_order_date", ACFdSQLRule.RuleCondition.GE, param.get("start_date", Timestamp.class));
                }//// change date to column name
                if(!param.isEmptyOrNull("end_date")) {
                wheres.and("purchase_order_date", ACFdSQLRule.RuleCondition.LT, new Timestamp(param.get("end_date", Long.class) + 24*60*60*1000));
                }
                //wheres.and("po_date", ACFdSQLRule.RuleCondition.LT, param.get("po_date_e", Timestamp.class));
            
            orders.put("purchase_order_date", false);
            return this;
        }
    }
    
    private class Search2 extends ACFgSearch {
        public Search2() {
            super();
        }
       }
    Search search = new Search();

    
    @RequestMapping(value=APPtMapping.APPF004_MAIN, method=RequestMethod.GET)
    public String main(ModelMap model) throws Exception {
        model.addAttribute("getSupplierCode", PoHeaderService.getSupplierCode());
        model.addAttribute("PurchaseOrderNo", PoHeaderService.getPurchaseOrderNo());
        //model.addAttribute("purchase_order_no", purchase_order_no);
        model.addAttribute("LocationCode", LocationService.getLocationCode());
       // model.addAttribute("item_no", item_no); //set row keys
        //model.addAttribute("unit_cost", unit_cost);
        //initial value in function maintenance form
        //model.addAttribute("modules", moduleService.getAllModuleValuePairs()); //acf's function, get data from ACFDB
        //System.out.println(moduleService.getAllModuleValuePairs());
        //search value groups in search form and main form
        //model.addAttribute("moduleGroups", funcGpService.getModuleFuncGpIndex()); // no need to group tables just now

        return view();
        
    }
    @RequestMapping(value=APPtMapping.APPF004_SEARCH_AJAX, method=RequestMethod.POST)
    @ResponseBody
    public ACFgResponseParameters getGrid(@RequestBody ACFgRequestParameters param) throws Exception {
      //The method getGrid responds to AJAX by obtain the Search JSON result and put in variable “grid_browse”.
        // ACF will forward the content to client and post to the grid which ID equals to “grid_browse”.
        search.setConnection(getConnection("ARCDB")); //get connection to the database
        search.setValues(param);
        search.setFocus(purchase_order_no); //set two keys
        System.out.println(param);
       // System.out.println(search.getGridResult());
        return new ACFgResponseParameters().set("grid_browse", search.getGridResult()); // can only be called once, otherwise reset parameter
    }
    
    @RequestMapping(value=APPtMapping.APPF004_GET_INVENTORY_TABLE_AJAX, method=RequestMethod.POST)
    @ResponseBody
    public ACFgResponseParameters getInventoryTable(@RequestBody ACFgRequestParameters param) throws Exception {                                
        ACFdSQLAssSelect select = new ACFdSQLAssSelect(); 
      //  select.setCustomSQL("");
      
        select.setCustomSQL("select * from(select I.*, IM.un_it, IM.item_description_1, PD.unit_cost, 0 as trigcrq, 0 as trigcbo, '' as current_received_quantity, '' as current_back_order_qty, I.received_quantity as pre_received_qty, I.back_order_quantity as pre_back_order_qty, "
                + " I.order_quantity - I.received_quantity - I.back_order_quantity as out_standing_quantity "
                + " from arc_item_inventory I, arc_item_master IM, arc_po_details PD "
                + " where I.purchase_order_no = PD.purchase_order_no "
                + " and I.item_no = PD.item_no "
                + " and I.item_no = IM.item_no"
                + " )");
        select.setKey("purchase_order_no");
        select.wheres.and("purchase_order_no", purchase_order_no);
        //select.orders.put("seq", true);
        return getResponseParameters().set("receipt_browse", select.executeGridQuery(getConnection("ARCDB"), param));
      
      }

    @RequestMapping(value=APPtMapping.APPF004_GET_FORM_AJAX, method=RequestMethod.POST)
    @ResponseBody
    public ACFgResponseParameters getForm(@RequestBody ACFgRequestParameters param) throws Exception {
        purchase_order_no = param.get("purchase_order_no", String.class); //pick the value of parameter “func_id” from client
        getInventoryTable(param.getRequestParameter("receipt_browse")); 
        //other_material = param.get("other_material", String.class);  //set two keys!!
        //unit_cost = param.get("unit_cost", BigDecimal.class);
        //retrieves the result by DAO, and put in the variable “frm_main”. 
        //ACF will forward the content to client and post to the form which ID equals to “frm_main”
        return getResponseParameters().set("frm_main", POHeaderDao.selectItem(purchase_order_no)); //change dao here //set two keys!!
    }
    @ACFgTransaction
    @RequestMapping(value=APPtMapping.APPF004_SAVE_AJAX, method=RequestMethod.POST)
    @ResponseBody
    public ACFgResponseParameters save(@RequestBody ACFgRequestParameters param) throws Exception { //function in the upper right "save" button
      //the controller obtains the changes of form data 
        List<ARCmPOHeader> amendments = param.getList("form", ARCmPOHeader.class);
       final String total = param.get("Total", String.class);
        final List<ARCmItemInventory> Inventoryamendments = param.getList("Receipt", ARCmItemInventory.class);
        System.out.println(Inventoryamendments);
        //and call DAO to save the changes
        ARCmPOHeader lastItem = POHeaderDao.saveItems(amendments, new ACFiSQLAssWriteInterface<ARCmPOHeader>(){
            
            
            //interface for the related functions
            @Override
            public boolean insert(ARCmPOHeader newItem, ACFdSQLAssInsert ass) throws Exception {
                //ass.columns.put("allow_print", 1); //without the allow_print column, the whole sql won't work
                ass.setAfterExecute(new ACFiCallback() {
                    @Override
                    public void callback() throws Exception {
                    	List<ARCmItemInventory>Inventoryamendments2 = new ArrayList<ARCmItemInventory>();
                    	for ( ARCmItemInventory each : Inventoryamendments)
                    	{
                    		Inventoryamendments2.add(each);
                    	}
                        if (Inventoryamendments2 != null)
                        	
//                            ItemInventoryDao.saveItems(Inventoryamendments);
                        	ItemInventoryDao.saveItems(Inventoryamendments2, new ACFiSQLAssWriteInterface<ARCmItemInventory>(){

								@Override
								public boolean insert(
										ARCmItemInventory newItem,
										ACFdSQLAssInsert ass) throws Exception {
									newItem.purchase_order_date = ACFtUtility.now();
 									newItem.receive_date = ACFtUtility.now();
 									System.out.println("testing ********************* newItem.receive_date **** i ***** insert" + newItem.receive_date);
									
 									// TODO Auto-generated method stub
									return false;
								}

								@Override
								public boolean update(
										ARCmItemInventory oldItem,
										ARCmItemInventory newItem,
										ACFdSQLAssUpdate ass) throws Exception {
									newItem.purchase_order_date = ACFtUtility.now();
 									newItem.receive_date = ACFtUtility.now();
 									System.out.println("testing ********************* newItem.receive_date **** i ***** update" + newItem.receive_date);
									// TODO Auto-generated method stub
									return false;
								}

								@Override
								public boolean delete(
										ARCmItemInventory oldItem,
										ACFdSQLAssDelete ass) throws Exception {
									// TODO Auto-generated method stub
									return false;
								}});
//                        Inventoryamendments.get(0).
                      
                    }
                });
                return false;
            }

            @Override
            public boolean update(ARCmPOHeader oldItem, ARCmPOHeader newItem, ACFdSQLAssUpdate ass) throws Exception {
                ass.setAfterExecute(new ACFiCallback() {
                    @SuppressWarnings("null")
					@Override
                    public void callback() throws Exception {
                        if (Inventoryamendments.size() != 0 ){
                        	int rcount = 0;
                        	int rcount1 = 0;
                        	int rcount2 = 0;
                        	System.out.println("testing List<ARCmItemInventory> InvAmend **************************" + Inventoryamendments + "Inventoryamendments.size()" + Inventoryamendments.size());
                        	List<ARCmItemInventory> InvAmend = Inventoryamendments;
                        	ACFdSQLAssSelect srch = new ACFdSQLAssSelect();
                        	//all receipt quantities should be added to "original item" as well as "item category +9999"
                        	//if record not exist, prompt error
                        	
//                        	//Search2 srch = new Search2();
                        	srch.setConnection(getConnection("ARCDB"));
                        	srch.setCustomSQL("select item_no from arc_item_master where "
                        			+ " material_type = '2'");
//                        	srch.wheres.and("material_type", 2);
//                        	srch.setValues(new ACFgRequestParameters());
                        	List<ACFgRawModel> result = srch.executeQuery();
                        	
                        	List<String> rs = new ArrayList<String>();
                        	for (ACFgRawModel i : result)
                        	{
                        		
                        	rs.add(i.getString("item_no"));
                        	}
                        	
                        	//get and add up all the received quantities
                        	for (ARCmItemInventory i : InvAmend) // this is only the modified records, not included the not modified ones
                        	{
                        		//minus those items' quantity don't belong to item_cat
                        		if((i.getVersion() == 1) && (rs.contains(i.item_no)))
                        		{
                        		rcount1 += i.received_quantity.intValue();
                        		}
                        		if((i.getVersion() == 2) && (rs.contains(i.item_no)))
                        		{
                        		rcount2 += i.received_quantity.intValue();
                        		}
                        		rcount = rcount2 - rcount1;
                        	}
                        	
//                        
                        	System.out.println("testing total ******************" + rcount);
                        	
                        	
                        	
                        	
                        	
                        	
                        	
                            //if inventory doesn't exist, created object and add object
                        	ARCmItemInventory is =new ARCmItemInventory();
                           	is.item_no = InvAmend.get(0).item_no.substring(0,3)+"9999";
                           	is.purchase_order_no = InvAmend.get(0).purchase_order_no;
                           	is.purchase_order_date = InvAmend.get(0).purchase_order_date;
                           	is.order_quantity = new BigDecimal(0);
                           	is.received_quantity = new BigDecimal(0);
                           	is.current_received_quantity = new BigDecimal(0);
                           	is.receive_date = InvAmend.get(0).receive_date;
                           	is.consumed_quantity = new BigDecimal(0);
                           	is.adjusted_quantity = new BigDecimal(0);
                           	is.back_order_quantity = new BigDecimal(0);
                           	is.current_back_order_quantity = new BigDecimal(0);
                           	is.unit_cost = new BigDecimal(0);
                           	is.setAction(1);
//                           	ls.add(is);
                           	
                           	//if the inventory existed, this step will be skipped and jump to add object
                           	if(ItemInventoryDao.selectItem(is.item_no, is.purchase_order_no)==null){
                           		
                           	ItemInventoryDao.insertOrUpdateItem(is);}
                           	
                           	//if the object is already exist add accepted object
                           	if(ItemInventoryDao.selectItem(InvAmend.get(0).item_no.substring(0,3) + "9999", InvAmend.get(0).purchase_order_no)!=null){
                            	ARCmItemInventory catitem = ItemInventoryDao.selectItem(InvAmend.get(0).item_no.substring(0,3) + "9999", InvAmend.get(0).purchase_order_no);
                            	ARCmItemInventory catitem1 = ItemInventoryDao.selectItem(InvAmend.get(0).item_no.substring(0,3) + "9999", InvAmend.get(0).purchase_order_no);
                            	
                            	if(catitem == null || catitem1== null)
                            	{
                            		throw exceptionService.error("APP305E");
                            	}

                            	ARCmItemInventory catitemv1 = (ARCmItemInventory) catitem1.setAction(2).setVersion(1);
//                            	catitemv1.received_quantity = new BigDecimal(0);
                            	InvAmend.add(catitemv1);
                            	ARCmItemInventory catitemv2 = (ARCmItemInventory) catitem.setAction(2).setVersion(2);
                            	catitemv2.received_quantity = new BigDecimal(catitemv1.received_quantity.intValue() + rcount);
                            	
                            	
                            	ACFdSQLAssSelect ass = new ACFdSQLAssSelect();
                                
                                ass.setCustomSQL("select d.item_no, d.purchase_order_no, d.unit_cost, im.material_type,"
                                		+ " im.item_no, d.modified_at from arc_po_details d, arc_item_master im "
                                		+ "where im.item_no = d.item_no "
//                                		+ "and im.material_type = '2' "
                                        + "and d.purchase_order_no = '%s' order by d.modified_at desc",InvAmend.get(0).purchase_order_no);
                                
                                List<ACFgRawModel> itemnos = ass.executeQuery(getConnection("ARCDB"));
                                System.out.println("tesing ************************************************* itemnos.get(0).get(unit_cost).toString()"+itemnos.get(0).get("unit_cost").toString());
                            	if (itemnos.get(0).get("material_type").toString().equals("2"))
                            	{
                                catitemv2.unit_cost = new BigDecimal (itemnos.get(0).get("unit_cost").toString()); //get unit cost from po
                            	InvAmend.add(catitemv2);
                            	System.out.println("tesing ***get(\"unit_cost\")********************************************** 2"+InvAmend);
                                ItemInventoryDao.saveItems(InvAmend);
                                }
                            	if (itemnos.get(0).get("material_type").toString().equals("1"))
                            	{
                                catitemv2.unit_cost = new BigDecimal (itemnos.get(0).get("unit_cost").toString()); //get unit cost from po
                            	InvAmend.add(catitemv2);
                            	System.out.println("tesing ***get(\"unit_cost\")********************************************** 1"+InvAmend);
                            	 ItemInventoryDao.saveItems(InvAmend);
//                            	 ItemInventoryDao.saveItems(InvAmend, new ACFiSQLAssWriteInterface<ARCmItemInventory>(){
//
//     								@Override
//     								public boolean insert(
//     										ARCmItemInventory newItem,
//     										ACFdSQLAssInsert ass) throws Exception {
//     									newItem.purchase_order_date = ACFtUtility.now();
//     									newItem.receive_date = ACFtUtility.now();
//     									System.out.println("testing ********************* newItem.receive_date **** u ***** insert" + newItem.receive_date);
//    									
//     									// TODO Auto-generated method stub
//     									return false;
//     								}
//
//     								@Override
//     								public boolean update(
//     										ARCmItemInventory oldItem,
//     										ARCmItemInventory newItem,
//     										ACFdSQLAssUpdate ass) throws Exception {
//     									newItem.purchase_order_date = ACFtUtility.now();
//     									newItem.receive_date = ACFtUtility.now();
//     									System.out.println("testing ********************* newItem.receive_date **** u ***** update" + newItem.receive_date);
//     									// TODO Auto-generated method stub
//     									return false;
//     								}
//
//     								@Override
//     								public boolean delete(
//     										ARCmItemInventory oldItem,
//     										ACFdSQLAssDelete ass) throws Exception {
//     									// TODO Auto-generated method stub
//     									return false;
//     								}});
//                            	ItemInventoryDao.updateItem(catitemv1, catitemv2);
                                }
//                                ItemInventoryDao.updateItem(catitemv1, catitemv2);
                            	}
//                            	
                            }
                        	
                        
                      
                    }
                });
                return false;
            }

            @Override
            public boolean delete(ARCmPOHeader oldItem, ACFdSQLAssDelete ass) throws Exception {
                return false;
            }
        });
        purchase_order_no = lastItem!=null? lastItem.purchase_order_no: null;// what's the purpose of this?
       // other_material = lastItem!=null? lastItem.other_material: null;
        //unit_cost = lastItem!=null? lastItem.unit_cost: null;

        return new ACFgResponseParameters();
    }

}