package io.github.riesenpilz.saveTags;

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
	
	default <Source> CompletableFuture<Source> b(Function<? super Mailbox<Source>, ? extends Msg> function) {
        CompletableFuture<Source> completablefuture = new CompletableFuture<Source>();

        completablefuture.getClass();
        Msg msg = function.apply(Mailbox.constructMailbox("ask future procesor handle", completablefuture::complete));

        this.setMessage(msg);
        return completablefuture;
    }
	default <Source> CompletableFuture<Source> c(Function<? super Mailbox<Either<Source, Exception>>, ? extends Msg> function) {
        CompletableFuture<Source> completablefuture = new CompletableFuture<Source>();
        Msg msg = function.apply(Mailbox.constructMailbox("ask future procesor handle", (either) -> {
            either.ifLeft(completablefuture::complete);
            either.ifRight(completablefuture::completeExceptionally);
        }));

        this.setMessage(msg);
        return completablefuture;
    }
}

