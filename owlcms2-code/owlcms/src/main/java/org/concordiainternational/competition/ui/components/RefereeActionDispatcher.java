package org.concordiainternational.competition.ui.components;

import org.concordiainternational.competition.data.Lifter;
import org.concordiainternational.competition.decision.IDecisionController;
import org.concordiainternational.competition.decision.RefereeDecisionController;
import org.concordiainternational.competition.ui.SessionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.event.Action;
import com.vaadin.event.Action.Notifier;
import com.vaadin.event.ShortcutAction;


public class RefereeActionDispatcher {

    private ShortcutActionListener action1ok;
    private ShortcutActionListener action1fail;
    private ShortcutActionListener action2ok;
    private ShortcutActionListener action2fail;
    private ShortcutActionListener action3ok;
    private ShortcutActionListener action3fail;
    private ShortcutActionListener startAction;
    private ShortcutActionListener stopAction;
    private ShortcutActionListener oneMinuteAction;
    private ShortcutActionListener twoMinutesAction;
    private ShortcutActionListener robotAction;
    private SessionData masterData;
    private Action.Notifier actionNotifier;
    private Thread robot;

    public final static Logger logger = LoggerFactory.getLogger(RefereeActionDispatcher.class);

    /**
     * @param masterData
     * @param mainWindow
     */
    public RefereeActionDispatcher(SessionData masterData, Notifier mainWindow) {
        super();
        this.masterData = masterData;
        this.actionNotifier = mainWindow;
    }

    @SuppressWarnings("serial")
    private abstract class ShortcutActionListener extends ShortcutAction implements Action.Listener {

        public ShortcutActionListener(String caption, int kc, int[] m) {
            super(caption, kc, m);
        }

        public ShortcutActionListener(String caption, int kc) {
            super(caption, kc, null);
        }

    }

    @SuppressWarnings("serial")
    public void addActions() {
        final IDecisionController refereeDecisionController = masterData.getRefereeDecisionController();
        action1ok = new ShortcutActionListener("1+", ShortcutAction.KeyCode.NUM1) {
            @Override
            public void handleAction(Object sender, Object target) {
                logger.debug("1+ {}",actionNotifier);
                refereeDecisionController.decisionMade(0, true);
            }
        };
        action1fail = new ShortcutActionListener("1-", ShortcutAction.KeyCode.NUM2) {
            @Override
            public void handleAction(Object sender, Object target) {
                logger.debug("1- {}",actionNotifier);
                refereeDecisionController.decisionMade(0, false);
            }
        };
        action2ok = new ShortcutActionListener("2+", ShortcutAction.KeyCode.NUM3) {
            @Override
            public void handleAction(Object sender, Object target) {
                logger.debug("2+ {}",actionNotifier);
                refereeDecisionController.decisionMade(1, true);
            }
        };
        action2fail = new ShortcutActionListener("2-", ShortcutAction.KeyCode.NUM4) {
            @Override
            public void handleAction(Object sender, Object target) {
                logger.debug("2- {}",actionNotifier);
                refereeDecisionController.decisionMade(1, false);
            }
        };
        action3ok = new ShortcutActionListener("3+", ShortcutAction.KeyCode.NUM5) {
            @Override
            public void handleAction(Object sender, Object target) {
                logger.debug("3+ {}",actionNotifier);
                refereeDecisionController.decisionMade(2, true);
            }
        };
        action3fail = new ShortcutActionListener("3-", ShortcutAction.KeyCode.NUM6) {
            @Override
            public void handleAction(Object sender, Object target) {
                logger.debug("3- {}",actionNotifier);
                refereeDecisionController.decisionMade(2, false);
            }
        };


        robotAction = new ShortcutActionListener("Robot", ShortcutAction.KeyCode.R) {
            /* Test mode.  For all lifts remaining in current group, call lifter and make a random decision.
             * Hitting "r" again stops this mode.
             * @see com.vaadin.event.Action.Listener#handleAction(java.lang.Object, java.lang.Object)
             */
            @Override
            public void handleAction(Object sender, Object target) {
                logger.debug("robot test {}",actionNotifier);
                final RefereeDecisionController refController = (RefereeDecisionController) masterData.getRefereeDecisionController();
                if (robot == null) {
                    robot = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                refController.setDecisionDisplayDelay(500);
                                refController.setDecisionReversalDelay(0);
                                refController.setResetDisplayDelay(2000);
                                Lifter curLifter = masterData.getCurrentLifter();
                                boolean done = false;
                                while (!done && curLifter != null &&  curLifter.getAttemptsDone() < 6) {
                                    masterData.callLifter(curLifter);

                                    // wait for the display and update to take place
                                    try {Thread.sleep(1000);} catch (InterruptedException e) {
                                        done = true;
                                    }
                                    if (!done) {
//                                        System.err.println(curLifter+" 1 announced="+masterData.isAnnounced());
                                        refController.decisionMade(0, Math.random() > 0.3);
//                                        System.err.println(curLifter+" 2 announced="+masterData.isAnnounced());
                                        refController.decisionMade(1, Math.random() > 0.3);
//                                        System.err.println(curLifter+" 3 announced="+masterData.isAnnounced());
                                        refController.decisionMade(2, Math.random() > 0.3);

                                        // wait for the display and update to take place
                                        try {Thread.sleep(3000);} catch (InterruptedException e) {
                                            done = true;
                                        }
                                        curLifter = masterData.getCurrentLifter();
                                    }
                                }
                            } finally {
                                refController.resetDecisionDisplayDelay();
                                refController.resetDecisionReversalDelay();
                                refController.resetResetDisplayDelay();
                                robot = null;
                            }
                        }
                    });
                    robot.start();
                } else {
                    robot.interrupt();
                }

            }
        };

        actionNotifier.addAction(action1ok);
        actionNotifier.addAction(action1fail);
        actionNotifier.addAction(action2ok);
        actionNotifier.addAction(action2fail);
        actionNotifier.addAction(action3ok);
        actionNotifier.addAction(action3fail);
        actionNotifier.addAction(robotAction);
    }

    public void removeActions() {
        actionNotifier.removeAction(startAction);
        actionNotifier.removeAction(stopAction);
        actionNotifier.removeAction(oneMinuteAction);
        actionNotifier.removeAction(twoMinutesAction);
        actionNotifier.removeAction(action1ok);
        actionNotifier.removeAction(action1fail);
        actionNotifier.removeAction(action2ok);
        actionNotifier.removeAction(action2fail);
        actionNotifier.removeAction(action3ok);
        actionNotifier.removeAction(action3fail);
        actionNotifier.removeAction(robotAction);
    }

}
