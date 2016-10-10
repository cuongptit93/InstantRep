package jp.co.efusion.aninstantreply;

class App extends android.app.Application
{
	private static App instance;
	public App()
	{
		instance = this;
	}
	public static App getInstance()
	{
		return instance;
	}

    final static InAppBillingSupporter inapp_billing_supporter = new InAppBillingSupporter();

}