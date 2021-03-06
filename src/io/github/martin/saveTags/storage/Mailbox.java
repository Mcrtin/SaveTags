package io.github.martin.saveTags.storage;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import com.mojang.datafixers.util.Either;

interface Mailbox<Msg> extends AutoCloseable {

	String getName();

	void setMessage(Msg msg);

	default void close() {
	}

	static <Msg> Mailbox<Msg> constructMailbox(final String name, final Consumer<Msg> consumer) {
		return new Mailbox<Msg>() {
			@Override
			public String getName() {
				return name;
			}

			@Override
			public void setMessage(Msg msg) {
				consumer.accept(msg);
			}

			public String toString() {
				return name;
			}
		};
	}
	
	default <Source> CompletableFuture<Source> send(Function<? super Mailbox<Source>, ? extends Msg> function) {
        CompletableFuture<Source> cf = new CompletableFuture<Source>();
        Msg msg = function.apply(Mailbox.constructMailbox("ask future procesor handle", cf::complete));
        setMessage(msg);
        return cf;
    }
	default <Source> CompletableFuture<Source> sendEither(Function<? super Mailbox<Either<Source, Exception>>, ? extends Msg> function) {
        CompletableFuture<Source> cf = new CompletableFuture<Source>();
        Msg msg = function.apply(Mailbox.constructMailbox("ask future procesor handle", (either) -> {
            either.ifLeft(cf::complete);
            either.ifRight(cf::completeExceptionally);
        }));
        setMessage(msg);
        return cf;
    }
}

