
Wp = 5;
Wm = 1;
Tp = 25;
Tm = 100;

 s1 = 20;
 s2 = 60;
 s3 = 80;
 s4 = 90;
 s5 = 95;
 s6 = 100;
 
 t1 = -100:0.001:0;
 t2 = 0:0.001:200;
 
 map = zeros(200, 3);
 %map(:, 2) = flipud((1:150)')./150;
 %map(:, 3) = flipud((1:150)')./150;
 map(:, 1) = 0.85.*((1:200)')./200;
 map(:, 2) = (.33-.45).*((1:200)')./200 + .45;
 map(:, 3) = (.1 - .74).* ((1:200)')./200 + .74;
 
 
 figure; hold;
 colormap(map);
 plot([-100 200], [0 0], 'k', 'LineWidth', 2);
 plot([0 0], [-6 6], 'k', 'LineWidth', 2);
 
 eVal = exp((s1-100)/15);
 wm = ((-Wp - Wm) * eVal) + Wm;
 tm = ((Tp - Tm) * eVal) + Tm;
 ys1 = Wp * exp(t1/Tp) - eVal;
 ys2 = -wm * exp(-t2/tm) - eVal;
 plot(t1, ys1, 'LineWidth', 2, 'Color', map(200, :)*1.15);
 plot(t2, ys2, 'LineWidth', 2, 'Color', map(1, :) *1.15);
 
 eVal = exp((s2-100)/15);
 wm = ((-Wp - Wm) * eVal) + Wm;
 tm = ((Tp - Tm) * eVal) + Tm;
 ys1 = Wp * exp(t1/Tp) - eVal;
 ys2 = -wm * exp(-t2/tm) - eVal;
 plot(t1(ys1 > 0), ys1(ys1>0), 'LineWidth', 2, 'Color', map(200, :)*1);
 plot(t1(ys1 < 0), ys1(ys1<0), 'LineWidth', 2, 'Color', map(1, :)*1);
 plot(t2(ys2 > 0), ys2(ys2>0), 'LineWidth', 2, 'Color', map(200, :)*1);
 plot(t2(ys2 < 0), ys2(ys2<0), 'LineWidth', 2, 'Color', map(1, :)*1);
 
 eVal = exp((s3-100)/15);
 wm = ((-Wp - Wm) * eVal) + Wm;
 tm = ((Tp - Tm) * eVal) + Tm;
 ys1 = Wp * exp(t1/Tp) - eVal;
 ys2 = -wm * exp(-t2/tm) - eVal;
 plot(t1(ys1 > 0), ys1(ys1>0), 'LineWidth', 2, 'Color', map(200, :)/1.25);
 plot(t1(ys1 < 0), ys1(ys1<0), 'LineWidth', 2, 'Color', map(1, :)/1.25);
 plot(t2(ys2 > 0), ys2(ys2>0), 'LineWidth', 2, 'Color', map(200, :)/1.25);
 plot(t2(ys2 < 0), ys2(ys2<0), 'LineWidth', 2, 'Color', map(1, :)/1.25);
 
 eVal = exp((s4-100)/15);
 wm = ((-Wp - Wm) * eVal) + Wm;
 tm = ((Tp - Tm) * eVal) + Tm;
 ys1 = Wp * exp(t1/Tp) - eVal;
 ys2 = -wm * exp(-t2/tm) - eVal;
 plot(t1(ys1 > 0), ys1(ys1>0), 'LineWidth', 2, 'Color', map(200, :)/1.5);
 plot(t1(ys1 < 0), ys1(ys1<0), 'LineWidth', 2, 'Color', map(1, :)/1.5);
 plot(t2(ys2 > 0), ys2(ys2>0), 'LineWidth', 2, 'Color', map(200, :)/1.5);
 plot(t2(ys2 < 0), ys2(ys2<0), 'LineWidth', 2, 'Color', map(1, :)/1.5);
 
 eVal = exp((s5-100)/15);
 wm = ((-Wp - Wm) * eVal) + Wm;
 tm = ((Tp - Tm) * eVal) + Tm;
 ys1 = Wp * exp(t1/Tp) - eVal;
 ys2 = -wm * exp(-t2/tm) - eVal;
 plot(t1(ys1 > 0), ys1(ys1>0), 'LineWidth', 2, 'Color', map(200, :)/1.75);
 plot(t1(ys1 < 0), ys1(ys1<0), 'LineWidth', 2, 'Color', map(1, :)/1.75);
 plot(t2(ys2 > 0), ys2(ys2>0), 'LineWidth', 2, 'Color', map(200, :)/1.75);
 plot(t2(ys2 < 0), ys2(ys2<0), 'LineWidth', 2, 'Color', map(1, :)/1.75);
 
  eVal = exp((s6-100)/15);
 wm = ((-Wp - Wm) * eVal) + Wm;
 tm = ((Tp - Tm) * eVal) + Tm;
 ys1 = Wp * exp(t1/Tp) - eVal;
 ys2 = -wm * exp(-t2/tm) - eVal;
 plot(t1(ys1 > 0), ys1(ys1>0), 'LineWidth', 2, 'Color', map(200, :)/2);
 plot(t1(ys1 < 0), ys1(ys1<0), 'LineWidth', 2, 'Color', map(1, :)/2);
 plot(t2(ys2 > 0), ys2(ys2>0), 'LineWidth', 2, 'Color', map(200, :)/2);
 plot(t2(ys2 < 0), ys2(ys2<0), 'LineWidth', 2, 'Color', map(1, :)/2);