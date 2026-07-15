package com.eu.habbo.habbohotel.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class QuestMidnightScheduler implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuestMidnightScheduler.class);
    private static final long MIN_DELAY_MS = 1000L;

    private final List<QuestMidnightTask> tasks;
    private volatile boolean disposed;

    public QuestMidnightScheduler() {
        this.tasks = Arrays.asList(
                new SeasonalQuestMidnightTask()
        );

        this.scheduleNext();
    }

    @Override
    public void run() {
        if (this.disposed)
            return;

        try {
            this.runTasks();
        } finally {
            this.scheduleNext();
        }
    }

    public void dispose() {
        this.disposed = true;
    }

    private void scheduleNext() {
        if (this.disposed || Emulator.getThreading() == null)
            return;

        Emulator.getThreading().run(this, this.getDelayUntilNextMidnight());
    }

    private long getDelayUntilNextMidnight() {
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(zone);

        return Math.max(MIN_DELAY_MS, Duration.between(now, nextMidnight).toMillis());
    }

    private void runTasks() {
        if (!QuestManager.isQuestSystemEnabled())
            return;

        QuestManager questManager = Emulator.getGameEnvironment().getQuestManager();

        for (QuestMidnightTask task : this.tasks) {
            if (!task.isEnabled())
                continue;

            int sent = 0;

            for (Map.Entry<Integer, Habbo> entry : Emulator.getGameEnvironment().getHabboManager().getOnlineHabbos().entrySet()) {
                Habbo habbo = entry.getValue();

                if (habbo == null || !habbo.isOnline() || habbo.getClient() == null)
                    continue;

                task.push(habbo, questManager);
                sent++;
            }

            LOGGER.info("Quest Midnight Scheduler -> Ran {} for {} online users.", task.getName(), sent);
        }
    }

}
