package com.poc.scenic;

import com.bea.wlevs.ede.api.EventType;
import com.bea.wlevs.ede.api.EventTypeRepository;
import com.bea.wlevs.ede.api.StreamSink;
import com.bea.wlevs.util.Service;

public class MyEventListener implements StreamSink {

	EventTypeRepository etr;
	
	int count = 0;

	@Service
	public void setEventTypeRepository(EventTypeRepository etr_) {
		etr = etr_;
	}

	public void onInsertEvent(Object event) {

		count ++;
		// Get the event type for the current event instance
		EventType eventType = etr.getEventType(event);
		// Get the event type name

		System.out.println( 
				//  eventType.getProperty("imsi").getValue(event) + ", " +
				// eventType.getProperty("imsi").getValue(event) + ", " +
				eventType.getProperty("imsi").getValue(event) + ", " +
				eventType.getProperty("staydays").getValue(event) + " Events no.: " + count);
	}
}
