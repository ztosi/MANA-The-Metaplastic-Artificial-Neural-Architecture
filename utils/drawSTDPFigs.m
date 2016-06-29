t_neg = -100:.01:0; %ms
t_neg_alt = -200:.01:0;
t_pos = 0:.01:200; %ms

blu = [0, .45, .74];
orng = [.85, .33, .1];

L_width = 3;

ee_wp = 5;
ee_wm = 1;
ee_tp = 30;
ee_tm = 100;

ei_wp = 5;
ei_wm = 2.25;
ei_tp = 30;
ei_tm = 60;

ie_wp = 1;
ie_wm = 1.25;
ie_sig = 30;
ie_a = 25;

ii_wp = .25;
ii_wm = 2.5;
ii_sig = 5;
ii_a = 25;


figure;

%% Ex Ex
subplot(221);
hold on;
plot([-200, 200], [0, 0], '-k', 'LineWidth', L_width);
plot([0, 0], [-6, 6], '-k', 'LineWidth', L_width);
plot(t_pos, -ee_wm.*exp(-t_pos./ee_tm), 'LineWidth', L_width);
plot(t_neg_alt, ee_wp.* exp(t_neg_alt./ee_tp), 'LineWidth', L_width);
hold off;


%% Ex In
subplot(222);
hold on;
plot([-200 200], [0 0], '-k', 'LineWidth', L_width);
plot([0 0], [-6 6], '-k', 'LineWidth', L_width);
plot(t_neg_alt, -ei_wm.* exp(t_neg_alt./ei_tm)+.25, 'LineWidth', L_width);
plot(t_pos, ee_wp.*exp(-t_pos./ee_tp), 'LineWidth', L_width);
hold off;

%% In Ex
subplot(223);
hold on;
plot([-200 200], [0 0], '-k', 'LineWidth', L_width);
plot([0 0], [-6 6], '-k', 'LineWidth', L_width);
t=-200:0.01:200;
vec = mexican_hat(t, ie_sig, ie_a);
zei = find(vec==0);
plot(t(1:zei(1)), ie_wm.*mexican_hat(t(1:zei(1)), ie_sig, ie_a), ...
    'LineWidth', L_width, 'Color', blu);
plot(t(zei(2):length(t)), ie_wm.*mexican_hat(t(zei(2):length(t)), ...
    ie_sig, ie_a), 'LineWidth', L_width, 'Color', blu);
plot(t(zei(1):zei(2)), ie_wp.*mexican_hat(t(zei(1):zei(2)), ie_sig, ie_a), ...
    'LineWidth', L_width, 'Color', orng);

%% In In
subplot(224);
hold on;
plot([-100 100], [0 0], '-k', 'LineWidth', L_width);
plot([0 0], [-12 6], '-k', 'LineWidth', L_width);
t=-100:0.01:100;
vec = mexican_hat(t, ii_sig, ii_a);
zei = find(vec==0);
plot(t(1:zei(1)), ii_wm.*mexican_hat(t(1:zei(1)), ii_sig, ii_a), ...
    'LineWidth', L_width, 'Color', blu);
plot(t(zei(2):length(t)), ii_wm.*mexican_hat(t(zei(2):length(t)), ...
    ii_sig, ii_a), 'LineWidth', L_width, 'Color', blu);
plot(t(zei(1):zei(2)), ii_wp.*mexican_hat(t(zei(1):zei(2)), ii_sig, ii_a), ...
    'LineWidth', L_width, 'Color', orng);


