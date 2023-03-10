/*
 * Copyright (C) 2009-2022 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.dispatch;

import akka.util.Unsafe;

final class AbstractMailbox {
  static final long mailboxStatusOffset;
  static final long systemMessageOffset;

  static {
    try {
      mailboxStatusOffset =
          Unsafe.instance.objectFieldOffset(
              Mailbox.class.getDeclaredField("_statusDoNotCallMeDirectly"));
      systemMessageOffset =
          Unsafe.instance.objectFieldOffset(
              Mailbox.class.getDeclaredField("_systemQueueDoNotCallMeDirectly"));
    } catch (Throwable t) {
      throw new ExceptionInInitializerError(t);
    }
  }
}
