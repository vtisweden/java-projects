package se.vti.roundtrips.common;

@FunctionalInterface
public interface Hook<S> {
	void run(S state, String... args) throws Exception;
}