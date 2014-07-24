package org.springframework.session;

/**
 * @author Arjen Poutsma
 */
public interface TimestampedSession extends Session {

	/**
	 * Gets the time when this session was created in milliseconds since midnight of
	 * 1/1/1970 GMT.
	 * @return the time when this session was created in milliseconds since midnight of
	 * 1/1/1970 GMT.
	 */
	long getCreationTime();

	/**
	 * Gets the last time this {@link Session} was accessed expressed in milliseconds
	 * since midnight of 1/1/1970 GMT
	 * @return the last time the client sent a request associated with the session
	 * expressed in milliseconds since midnight of 1/1/1970 GMT
	 */
	long getLastAccessedTime();

	/**
	 * Allows setting the last time this {@link Session} was accessed.
	 * @param lastAccessedTime the last time the client sent a request associated with the
	 * session expressed in milliseconds since midnight of 1/1/1970 GMT
	 */
	void setLastAccessedTime(long lastAccessedTime);

	/**
	 * Gets the maximum inactive interval in seconds between requests before this session
	 * will be invalidated. A negative time indicates that the session will never
	 * timeout.
	 * @return the maximum inactive interval in seconds between requests before this
	 * session will be invalidated. A negative time indicates that the session will never
	 * timeout.
	 */
	int getMaxInactiveInterval();

	/**
	 * Sets the maximum inactive interval in seconds between requests before this session
	 * will be invalidated. A negative time indicates that the session will never
	 * timeout.
	 * @param interval the number of seconds that the {@link Session} should be kept alive
	 * between client requests.
	 */
	void setMaxInactiveInterval(int interval);

}
