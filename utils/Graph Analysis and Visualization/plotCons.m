figure;
hold on;
[kIn, kOut, k] = nodeDegrees(wts);
[ksrt,sind] = sort(k, 'descend');
cut = uint32(length(k)/20);
w2 = wts;
w2(sind(1:cut), sind(1:cut)) = 0;
w2 = w2 ~= 0;
w2 = (w2+w2')~=0;
w2 = triu(w2,1);
[I,J] = find(w2~=0);
for i=1:length(I)
   plot([positions(I(i),3), positions(J(i),3)]+ ...
       [positions(I(i),1), positions(J(i),1)], ...
       [positions(I(i),2),  positions(J(i),2)]+ ...
       [positions(I(i),1), positions(J(i),1)], 'Color',[.6 , .6, .6], ...
       'LineWidth', .5);
end
w2 = wts;
w2(sind(cut:length(sind)), sind(cut:length(sind))) = 0;
w2 = w2 ~= 0;
w2 = (w2+w2')~=0;
w2 = triu(w2,1);
[I,J] = find(w2~=0);
for i=1:length(I)
   plot([positions(I(i),3), positions(J(i),3)]+ ...
       [positions(I(i),1), positions(J(i),1)], ...
       [positions(I(i),2),  positions(J(i),2)]+ ...
       [positions(I(i),1), positions(J(i),1)], 'g', ...
       'LineWidth', 2);
end
 scatter(positions(:,3) + positions(:,1), positions(:,2) + positions(:,1),30, ...
    [.5 .5 .5], 'filled');
hold off;