package org.springframework.session.data.redis;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.session.data.redis.RedisOperationsSessionRepository.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.session.MapSession;
import org.springframework.session.Session;
import org.springframework.session.TimestampedSession;
import org.springframework.session.data.redis.RedisOperationsSessionRepository.RedisSession;


@RunWith(MockitoJUnitRunner.class)
public class RedisOperationsSessionRepositoryTests {
    @Mock
    RedisOperations<String,Session> redisOperations;
    @Mock
    BoundHashOperations<String, Object, Object> boundHashOperations;
    @Captor
    ArgumentCaptor<Map<String,Object>> delta;

    private RedisOperationsSessionRepository redisRepository;

    @Before
    public void setup() {
        this.redisRepository = new RedisOperationsSessionRepository(redisOperations);
    }

    @Test
    public void createSessionDefaultMaxInactiveInterval() throws Exception {
        TimestampedSession session = redisRepository.createSession();
        assertThat(session.getMaxInactiveInterval()).isEqualTo(new MapSession().getMaxInactiveInterval());
    }

    @Test
    public void createSessionCustomMaxInactiveInterval() throws Exception {
        int interval = 1;
        redisRepository.setDefaultMaxInactiveInterval(interval);
	    TimestampedSession session = redisRepository.createSession();
        assertThat(session.getMaxInactiveInterval()).isEqualTo(interval);
    }

    @Test
    public void saveNewSession() {
        RedisSession session = redisRepository.createSession();
        when(redisOperations.boundHashOps(getKey(session.getId()))).thenReturn(boundHashOperations);

        redisRepository.save(session);

        Map<String,Object> delta = getDelta();
        assertThat(delta.size()).isEqualTo(3);
        Object creationTime = delta.get(CREATION_TIME_ATTR);
        assertThat(creationTime).isInstanceOf(Long.class);
        assertThat(delta.get(MAX_INACTIVE_ATTR)).isEqualTo(MapSession.DEFAULT_MAX_INACTIVE_INTERVAL_SECONDS);
        assertThat(delta.get(LAST_ACCESSED_ATTR)).isEqualTo(creationTime);
    }

    @Test
    public void saveLastAccessChanged() {
        RedisSession session = redisRepository.new RedisSession(new MapSession());
        session.setLastAccessedTime(12345678L);
        when(redisOperations.boundHashOps(getKey(session.getId()))).thenReturn(boundHashOperations);

        redisRepository.save(session);

        assertThat(getDelta()).isEqualTo(map(LAST_ACCESSED_ATTR, session.getLastAccessedTime()));
    }

    @Test
    public void saveSetAttribute() {
        String attrName = "attrName";
        RedisSession session = redisRepository.new RedisSession(new MapSession());
        session.setAttribute(attrName, "attrValue");
        when(redisOperations.boundHashOps(getKey(session.getId()))).thenReturn(boundHashOperations);

        redisRepository.save(session);

        assertThat(getDelta()).isEqualTo(map(getSessionAttrNameKey(attrName), session.getAttribute(attrName)));
    }

    @Test
    public void saveRemoveAttribute() {
        String attrName = "attrName";
        RedisSession session = redisRepository.new RedisSession(new MapSession());
        session.removeAttribute(attrName);
        when(redisOperations.boundHashOps(getKey(session.getId()))).thenReturn(boundHashOperations);

        redisRepository.save(session);

        assertThat(getDelta()).isEqualTo(map(getSessionAttrNameKey(attrName), null));
    }

    @Test
    public void redisSessionGetAttributes() {
        String attrName = "attrName";
        RedisSession session = redisRepository.new RedisSession(new MapSession());
        assertThat(session.getAttributeNames()).isEmpty();
        session.setAttribute(attrName, "attrValue");
        assertThat(session.getAttributeNames()).containsOnly(attrName);
        session.removeAttribute(attrName);
        assertThat(session.getAttributeNames()).isEmpty();
    }

    @Test
    public void delete() {
        String id = "abc";
        redisRepository.delete(id);
        verify(redisOperations).delete(getKey(id));
    }

    @Test
    public void getSessionNotFound() {
        String id = "abc";
        when(redisOperations.boundHashOps(getKey(id))).thenReturn(boundHashOperations);
        when(boundHashOperations.entries()).thenReturn(map());

        assertThat(redisRepository.getSession(id)).isNull();
    }

    @Test
    public void getSessionFound() {
        String attrName = "attrName";
        MapSession expected = new MapSession();
        expected.setAttribute(attrName, "attrValue");
        when(redisOperations.boundHashOps(getKey(expected.getId()))).thenReturn(boundHashOperations);
        Map map = map(
                getSessionAttrNameKey(attrName), expected.getAttribute(attrName),
                CREATION_TIME_ATTR, expected.getCreationTime(),
                MAX_INACTIVE_ATTR, expected.getMaxInactiveInterval(),
                LAST_ACCESSED_ATTR, expected.getLastAccessedTime());
        when(boundHashOperations.entries()).thenReturn(map);

        RedisSession session = redisRepository.getSession(expected.getId());
        assertThat(session.getId()).isEqualTo(expected.getId());
        assertThat(session.getAttributeNames()).isEqualTo(expected.getAttributeNames());
        assertThat(session.getAttribute(attrName)).isEqualTo(expected.getAttribute(attrName));
        assertThat(session.getCreationTime()).isEqualTo(expected.getCreationTime());
        assertThat(session.getMaxInactiveInterval()).isEqualTo(expected.getMaxInactiveInterval());
        assertThat(session.getLastAccessedTime()).isEqualTo(expected.getLastAccessedTime());

    }

    private Map map(Object...objects) {
        Map<String,Object> result = new HashMap<String,Object>();
        if(objects == null) {
            return result;
        }
        for(int i = 0; i < objects.length; i += 2) {
            result.put((String)objects[i], objects[i+1]);
        }
        return result;
    }

    private Map<String,Object> getDelta() {
        verify(boundHashOperations).putAll(delta.capture());
        return delta.getValue();
    }
}