package winston.cep.esper;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

public class MyEventListener implements UpdateListener {
	
	int count = 0;

	@Override
	public void update(EventBean[] newEvents, EventBean[] oldEvents) {
		
		count ++;
		
		EventBean event = newEvents[0];
		System.out.println(
				event.get("imsi") + ", " +
				//event.get("mytime") + ", " +
				event.get("staydays") + ", Events no.: " + count);
	}

}
