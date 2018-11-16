package net.shadowxcraft.rollbackcore;

import org.bukkit.Location;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.util.Getter;
import net.shadowxcraft.rollbackcore.events.ClearEntitiesEndEvent;
import net.shadowxcraft.rollbackcore.events.CopyEndEvent;
import net.shadowxcraft.rollbackcore.events.PasteEndEvent;
import net.shadowxcraft.rollbackcore.events.WDImportEndEvent;
import net.shadowxcraft.rollbackcore.events.WDRollbackEndEvent;

public class SkriptSupport {
	public static void initSkriptSupport() {
		// Register events
		Skript.registerEvent("RollbackCore Copy End", SimpleEvent.class, CopyEndEvent.class, "[rollbackcore] copy end");
		EventValues.registerEventValue(CopyEndEvent.class, String.class, new Getter<String, CopyEndEvent>() {
			@Override
			public String get(CopyEndEvent e) {
				return e.endStatus().name();
			}
		}, 0);

		EventValues.registerEventValue(CopyEndEvent.class, Location.class, new Getter<Location, CopyEndEvent>() {
			@Override
			public Location get(CopyEndEvent e) {
				return e.getCopy().getMin();
			}
		}, 0);

		Skript.registerEvent("RollbackCore Paste End", SimpleEvent.class, PasteEndEvent.class,
				"[rollbackcore] paste end");
		EventValues.registerEventValue(PasteEndEvent.class, Location.class, new Getter<Location, PasteEndEvent>() {
			@Override
			public Location get(PasteEndEvent e) {
				return e.getPaste().getMin();
			}
		}, 0);
		EventValues.registerEventValue(PasteEndEvent.class, String.class, new Getter<String, PasteEndEvent>() {
			@Override
			public String get(PasteEndEvent e) {
				return e.endStatus().name();
			}
		}, 0);

		Skript.registerEvent("RollbackCore Clear Entities End", SimpleEvent.class, ClearEntitiesEndEvent.class,
				"[rollbackcore] entityclear end");
		EventValues.registerEventValue(ClearEntitiesEndEvent.class, String.class,
				new Getter<String, ClearEntitiesEndEvent>() {
					@Override
					public String get(ClearEntitiesEndEvent e) {
						return e.endStatus().name();
					}
				}, 0);
		EventValues.registerEventValue(ClearEntitiesEndEvent.class, Location.class,
				new Getter<Location, ClearEntitiesEndEvent>() {
					@Override
					public Location get(ClearEntitiesEndEvent e) {
						return e.getClearEntitiesObject().min;
					}
				}, 0);

		Skript.registerEvent("RollbackCore WatchDog Import End", SimpleEvent.class, WDImportEndEvent.class,
				"[rollbackcore] wdimport end");
		EventValues.registerEventValue(WDImportEndEvent.class, String.class, new Getter<String, WDImportEndEvent>() {
			@Override
			public String get(WDImportEndEvent e) {
				return e.endStatus().name();
			}
		}, 0);
		EventValues.registerEventValue(WDImportEndEvent.class, Location.class,
				new Getter<Location, WDImportEndEvent>() {
					@Override
					public Location get(WDImportEndEvent e) {
						return e.getWatchDog().getMin();
					}
				}, 0);

		Skript.registerEvent("RollbackCore WatchDog Rollback End", SimpleEvent.class, WDRollbackEndEvent.class,
				"[rollbackcore] wdrollback end");
		EventValues.registerEventValue(WDRollbackEndEvent.class, String.class,
				new Getter<String, WDRollbackEndEvent>() {
					@Override
					public String get(WDRollbackEndEvent e) {
						return e.endStatus().name();
					}
				}, 0);
		EventValues.registerEventValue(WDRollbackEndEvent.class, Location.class,
				new Getter<Location, WDRollbackEndEvent>() {
					@Override
					public Location get(WDRollbackEndEvent e) {
						return e.getWatchDog().getMin();
					}
				}, 0);
	}
}
