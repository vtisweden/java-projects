package se.vti.roundtrips.common;

@FunctionalInterface
public interface Hook {
	void run(String... args);
}