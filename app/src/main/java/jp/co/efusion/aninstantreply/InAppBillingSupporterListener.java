
package jp.co.efusion.aninstantreply;

public interface InAppBillingSupporterListener
{
	  public void inventoryChecked(String sku_id);
	  public void purchased(String sku_id, boolean isTest);

}
