package net.minecraft.resources.event;

import net.minecraft.logging.Log;
import net.minecraft.resources.Domain;
import net.minecraft.resources.event.events.Cancelable;
import net.minecraft.util.Unsafe;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/*
 * Всегда пользуйтесь Registrar, абсолютно всегда
 * Никогда не пользуйтесь этим классом если не уверены в том что делаете
 */
@Unsafe
public class EventManager<T extends Event> {
	private Listener<T>[] array;
	private boolean using = false;

    @SuppressWarnings("unchecked")
	public void add(Listener<T> listener) {
    	if(!using){
    		array = new Listener[]{listener};
			using = true;
    		return;
		}
		using = true;

    	int priority = listener.priority();
    	for(int i = 0; i < array.length; i++){
    		Listener l = array[i];
    		if(priority <= l.priority()){
    			array = ArrayUtils.add(array, i, listener);
    			return;
			}
		}
		array = ArrayUtils.add(array, listener);
    }

	@SuppressWarnings("unchecked")
	public void remove(Listener<T> listener) {
		if(!using) return;
		int where = ArrayUtils.indexOf(array, listener);
		if(where == -1)return;
		if(array.length == 1){
		    array = null;
		    using = false;
		    return;
        }
		array = ArrayUtils.remove(array, where);
	}

	public T call(T event) {
		if (!using) return event;
		for (Listener<T> listener : array){
		    if(listener.ignoreCancelled() && event instanceof Cancelable && ((Cancelable)event).isCanceled())
                continue;
		    try{
				listener.process(event);
			}catch (Throwable throwable){
		    	Log.MAIN.error("Error on event " + event + " listener " + listener);
				Log.MAIN.exception(throwable);
			}
        }
		return event;
	}

	public boolean isUseful() {
		return using;
	}
}
