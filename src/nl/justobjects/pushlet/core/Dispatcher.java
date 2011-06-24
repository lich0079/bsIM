// Copyright (c) 2000 Just Objects B.V. <just@justobjects.nl>
// Distributable under LGPL license. See terms of license at gnu.org.

package nl.justobjects.pushlet.core;

import nl.justobjects.pushlet.util.Log;

/**
 * Routes Events to Subscribers.
 *
 * @author Just van den Broecke - Just Objects &copy;
 * @version $Id: Dispatcher.java,v 1.8 2007/11/23 14:33:07 justb Exp $
 */
//这2个接口都是用来存储字符串常量的  没有方法定义
public class Dispatcher implements Protocol, ConfigDefs {
	/**
	 * Singleton pattern:  single instance.
	 */
	private static Dispatcher instance;

	static {
		try {
		    //静态初始化方法 去配置文件找实例 要不就默认的nl.justobjects.pushlet.core.Dispatcher
		    //此处可以实现配置扩展新的Dispatcher类
			instance = (Dispatcher) Config.getClass(DISPATCHER_CLASS, "nl.justobjects.pushlet.core.Dispatcher").newInstance();
			Log.info("Dispatcher created className=" + instance.getClass());
		} catch (Throwable t) {
			Log.fatal("Cannot instantiate Dispatcher from config", t);
		}
	}

	/**
	 * Singleton pattern with factory method: protected constructor.
	 */
	protected Dispatcher() {
	}

	/**
	 * Singleton pattern: get single instance.
	 */
	public static Dispatcher getInstance() {
		return instance;
	}

	/**
	 * Send event to all subscribers.
	 */
	//向session中所有订阅者广播事件，每个都是复制的事件
	public synchronized void broadcast(Event event) {
		// Get active sessions
		Session[] sessions = getSessions();

		// Send Event to all Subscribers
		for (int i = 0; i < sessions.length; i++) {

			// Snapshot array may not be filled entirely.
			if (sessions[i] == null) {
				break;
			}
			sessions[i].getSubscriber().onEvent((Event) event.clone());
		}
	}

	/**
	 * Send event to subscribers matching Event subject.
	 */
	//向订阅了该事件的人发送  并设置事件的订阅ID 和label
	public synchronized void multicast(Event event) {
		// Get snapshot active sessions
		Session[] sessions = getSessions();

		// Send Event to all Subscribers whose Subject match Event
		Event clonedEvent = null;
		Subscription subscription = null;
		Subscriber subscriber = null;
		for (int i = 0; i < sessions.length; i++) {

			// Snapshot array may not be filled entirely.
			if (sessions[i] == null) {
				break;
			}

			subscriber = sessions[i].getSubscriber();

			// Send only if the subscriber's criteria
			// match the event.
			if ((subscription = subscriber.match(event)) != null) {
				// Personalize event
				clonedEvent = (Event) event.clone();

				// Set subscription id and optional label
				clonedEvent.setField(P_SUBSCRIPTION_ID, subscription.getId());
				if (subscription.getLabel() != null) {
					event.setField(P_SUBSCRIPTION_LABEL, subscription.getLabel());
				}

				subscriber.onEvent(clonedEvent);
			}
		}

	}

	/**
	 * Send event to specific subscriber.
	 */
	//向指定的sessionID发送事件
	public synchronized void unicast(Event event, String aSessionId) {
		// Get subscriber to send event to
		Session session = SessionManager.getInstance().getSession(aSessionId);
		if (session == null) {
			Log.warn("unicast: session with id=" + aSessionId + " does not exist");
			return;
		}

		// Send Event to subscriber.
		session.getSubscriber().onEvent((Event) event.clone());
	}

	/**
	 * Start Dispatcher.
	 */
	public void start() {
		Log.info("Dispatcher started");
	}

	/**
	 * Stop Dispatcher.
	 */
	//stop的时候向所有session发送E_ABORT事件
	public void stop() {
		// Send abort control event to all subscribers.
		Log.info("Dispatcher stopped: broadcast abort to all subscribers");
		broadcast(new Event(E_ABORT));
	}

	private Session[] getSessions() {
		return SessionManager.getInstance().getSnapshot();
	}
}

/*
 * $Log: Dispatcher.java,v $
 * Revision 1.8  2007/11/23 14:33:07  justb
 * core classes now configurable through factory
 *
 * Revision 1.7  2005/02/28 12:45:59  justb
 * introduced Command class
 *
 * Revision 1.6  2005/02/28 09:14:55  justb
 * sessmgr/dispatcher factory/singleton support
 *
 * Revision 1.5  2005/02/21 16:59:06  justb
 * SessionManager and session lease introduced
 *
 * Revision 1.4  2005/02/21 11:50:46  justb
 * ohase1 of refactoring Subscriber into Session/Controller/Subscriber
 *
 * Revision 1.3  2005/02/18 12:36:47  justb
 * changes for renaming and configurability
 *
 * Revision 1.2  2005/02/18 10:07:23  justb
 * many renamings of classes (make names compact)
 *
 * Revision 1.1  2005/02/18 09:54:15  justb
 * refactor: rename Publisher Dispatcher and single Subscriber class
 *
 * Revision 1.14  2005/02/16 14:39:34  justb
 * fixed leave handling and added "poll" mode
 *
 * Revision 1.13  2004/10/24 20:50:35  justb
 * refine subscription with label and sending sid and label on events
 *
 * Revision 1.12  2004/10/24 12:58:18  justb
 * revised client and test classes for new protocol
 *
 * Revision 1.11  2004/09/26 21:39:43  justb
 * allow multiple subscriptions and out-of-band requests
 *
 * Revision 1.10  2004/09/20 22:01:38  justb
 * more changes for new protocol
 *
 * Revision 1.9  2004/09/03 22:35:37  justb
 * Almost complete rewrite, just checking in now
 *
 * Revision 1.8  2004/08/13 23:36:05  justb
 * rewrite of Pullet into Pushlet "pull" mode
 *
 * Revision 1.7  2004/08/12 13:18:54  justb
 * cosmetic changes
 *
 * Revision 1.6  2004/03/10 15:45:55  justb
 * many cosmetic changes
 *
 * Revision 1.5  2004/03/10 13:59:28  justb
 * rewrite using Collection classes and finer synchronization
 *
 * Revision 1.4  2003/08/15 08:37:40  justb
 * fix/add Copyright+LGPL file headers and footers
 *
 * Revision 1.3  2003/08/12 08:54:40  justb
 * added getSubscriberCount() and use Log
 *
 * Revision 1.2  2003/05/18 16:15:08  justb
 * support for XML encoded Events
 *
 * Revision 1.1.1.1  2002/09/24 21:02:31  justb
 * import to sourceforge
 *
 * Revision 1.1.1.1  2002/09/20 22:48:18  justb
 * import to SF
 *
 * Revision 1.1.1.1  2002/09/20 14:19:04  justb
 * first import into SF
 *
 * Revision 1.3  2002/04/15 20:42:41  just
 * reformatting and renaming GuardedQueue to EventQueue
 *
 * Revision 1.2  2000/08/21 20:48:29  just
 * added CVS log and id tags plus copyrights
 *
 *
 */
